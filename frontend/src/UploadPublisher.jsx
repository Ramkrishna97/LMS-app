import React, { useEffect, useState } from 'react';
import ImageCropper from './ImageCropper.jsx';
import QuizBuilder, { blankQuestion } from './QuizBuilder.jsx';
import { apiUrl } from './api.js';

async function send(path, body, token, json = false) {
  let response;
  try {
    response = await fetch(apiUrl(path), {
      method: 'POST', body, headers: { 'X-Auth-Token': token, ...(json ? { 'Content-Type': 'application/json' } : {}) }
    });
  } catch {
    throw new Error('Could not reach the server. Check that the application is running, then try again.');
  }
  const data = await response.json().catch(() => null);
  if (!response.ok) throw new Error(data?.message || `Upload failed (HTTP ${response.status}).`);
  return data;
}

export default function UploadPublisher({ session, done, onAuthExpired }) {
  const [type, setType] = useState('POST');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [file, setFile] = useState(null);
  const [cropSource, setCropSource] = useState(null);
  const [sourceName, setSourceName] = useState('');
  const [notice, setNotice] = useState('');
  const [uploading, setUploading] = useState(false);
  const [quizMode, setQuizMode] = useState('manual');
  const [quiz, setQuiz] = useState({ title: '', description: '', durationMinutes: 30, questions: [blankQuestion()] });
  useEffect(() => () => { if (cropSource) URL.revokeObjectURL(cropSource); }, [cropSource]);

  const selectFile = async event => {
    const selected = event.target.files?.[0];
    if (!selected) return;
    event.target.value = '';
    setNotice('');
    if (type !== 'IMAGE' || selected.type === 'image/gif') { setFile(selected); return; }
    setFile(null);
    const url = URL.createObjectURL(selected);
    const preview = new Image();
    preview.src = url;
    try {
      await preview.decode();
      setSourceName(selected.name);
      setCropSource(url);
    } catch {
      URL.revokeObjectURL(url);
      setFile(null);
      setNotice('This image cannot be previewed by your browser. Convert it to PNG or JPEG and try again.');
    }
  };

  const publish = async event => {
    event.preventDefault();
    if (uploading) return;
    setNotice('');
    setUploading(true);
    try {
      if (type === 'QUIZ') {
        if (quizMode === 'import') {
          if (!file) throw new Error('Choose a quiz JSON file.');
          const data = new FormData(); data.append('file', file);
          await send('/quizzes/import', data, session.token);
        } else {
          await send('/quizzes', JSON.stringify({ ...quiz, durationMinutes: Number(quiz.durationMinutes),
            questions: quiz.questions.map(question => ({ ...question, correctOption: Number(question.correctOption) })) }), session.token, true);
        }
      } else if (type === 'IMAGE' || type === 'DOCUMENT') {
        if (!file) throw new Error(type === 'IMAGE' ? 'Choose and crop an image first.' : 'Choose a document first.');
        const data = new FormData();
        data.append('title', title.trim()); data.append('description', description.trim());
        data.append('type', type); data.append('file', file);
        await send('/contents/upload', data, session.token);
      } else {
        await send('/contents', JSON.stringify({ title: title.trim(), body: description.trim(), type }), session.token, true);
      }
      done();
    } catch (error) {
      if (error.message === 'Please sign in') onAuthExpired();
      else setNotice(error.message);
    }
    finally { setUploading(false); }
  };

  return <>
    <form className="form-panel" onSubmit={publish}>
      <h2>Upload & publish</h2>
      <p className="text-secondary">Publish a post, document, image, or quiz from your computer.</p>
      {notice && <div className="alert alert-danger" role="alert">{notice}</div>}
      <label className="form-label" htmlFor="content-type">Content type</label>
      <select id="content-type" className="form-select mb-3" value={type} onChange={e => {
        setType(e.target.value); setFile(null); setCropSource(null); setNotice('');
      }}><option>POST</option><option>DOCUMENT</option><option>IMAGE</option><option>QUIZ</option></select>
      {type !== 'QUIZ' && <>
        <label className="form-label" htmlFor="content-title">Title</label>
        <input id="content-title" className="form-control mb-3" required value={title} onChange={e => setTitle(e.target.value)} />
        <label className="form-label" htmlFor="content-description">Description / content</label>
        <textarea id="content-description" className="form-control mb-3" rows="4" required value={description} onChange={e => setDescription(e.target.value)} />
      </>}
      {type === 'QUIZ' && <div className="d-flex gap-2 mb-3" role="group" aria-label="Quiz creation method">
        <button type="button" className={'btn ' + (quizMode === 'manual' ? 'btn-primary' : 'btn-outline-primary')}
          onClick={() => { setQuizMode('manual'); setNotice(''); }}>Build manually</button>
        <button type="button" className={'btn ' + (quizMode === 'import' ? 'btn-primary' : 'btn-outline-primary')}
          onClick={() => { setQuizMode('import'); setNotice(''); }}>Import JSON</button>
      </div>}
      {type === 'QUIZ' && quizMode === 'manual' && <QuizBuilder quiz={quiz} setQuiz={setQuiz} />}
      {(type === 'DOCUMENT' || type === 'IMAGE' || (type === 'QUIZ' && quizMode === 'import')) && <div className="upload-zone">
        <i className="bi bi-cloud-arrow-up" />
        <h5>{type === 'QUIZ' ? 'Import quiz JSON' : type === 'IMAGE' ? 'Choose and crop image' : 'Upload document'}</h5>
        <p>{type === 'IMAGE' ? 'Crop the source image; the result is optimized before upload.' : 'Select a local file to publish.'}</p>
        <input key={type} className="form-control" type="file"
          accept={type === 'QUIZ' ? '.json' : type === 'IMAGE' ? 'image/*' : '.pdf,.doc,.docx,.xls,.xlsx,.txt'}
          required={!file} onChange={selectFile} />
        {file && <small className="text-success d-block mt-2">Ready: {file.name} ({Math.ceil(file.size / 1024)} KB)</small>}
        {type === 'IMAGE' && file?.type === 'image/gif' && <div className="mt-3">
          <small className="d-block text-secondary mb-2">Animation is preserved if you publish this GIF as-is. Cropping converts it to a still JPEG.</small>
          <button type="button" className="btn btn-outline-primary btn-sm" onClick={() => { setSourceName(file.name); setCropSource(URL.createObjectURL(file)); }}>Crop first frame</button>
        </div>}
      </div>}
      <button className="btn btn-primary mt-4 px-4" disabled={uploading}>
        <i className="bi bi-upload" /> {uploading ? 'Uploading…' : 'Upload and publish'}
      </button>
      {type === 'QUIZ' && quizMode === 'import' && <div className="schema-example mt-4">Required: title, description, durationMinutes, questions[]. Each question needs questionText, optionA–D, correctOption (0–3), and solution.</div>}
    </form>
    {cropSource && <ImageCropper source={cropSource} name={sourceName}
      onCancel={() => setCropSource(null)} onDone={cropped => { setFile(cropped); setCropSource(null); }} />}
  </>;
}
