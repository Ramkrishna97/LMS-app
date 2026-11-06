import React, { useEffect, useState } from 'react';
import ContentDetails from './ContentDetails.jsx';
import { apiUrl } from './api.js';

const icons = { POST: 'bi-file-text', QUIZ: 'bi-patch-question', DOCUMENT: 'bi-file-earmark-pdf', IMAGE: 'bi-image' };

export default function AuthenticatedDashboard({ session, QuizComponent }) {
  const [items, setItems] = useState([]);
  const [selected, setSelected] = useState(null);
  const [quiz, setQuiz] = useState(null);
  const [takingQuiz, setTakingQuiz] = useState(false);
  const [error, setError] = useState('');
  useEffect(() => {
    fetch(apiUrl('/dashboard')).then(r => { if (!r.ok) throw new Error('Could not load the dashboard.'); return r.json(); })
      .then(setItems).catch(e => setError(e.message));
  }, []);
  const open = async item => {
    setError(''); setSelected(item); setQuiz(null); setTakingQuiz(false);
    if (item.type !== 'QUIZ') return;
    try {
      const response = await fetch(apiUrl(`/quizzes/content/${item.id}`));
      if (!response.ok) throw new Error('Could not load quiz details.');
      setQuiz(await response.json());
    } catch (e) { setError(e.message); }
  };
  const completed = result => {
    setQuiz(current => current ? { ...current, attemptCount: result.attemptCount, maxScore: result.maxScore } : current);
    setItems(current => current.map(item => item.id === quiz?.contentId
      ? { ...item, quizAttemptCount: result.attemptCount, quizMaxScore: result.maxScore }
      : item));
  };
  const close = () => { setSelected(null); setQuiz(null); setTakingQuiz(false); };
  return <>
    <div className="welcome"><div><span className="eyebrow">WELCOME BACK</span>
      <h1>Keep learning, {session.user.name.split(' ')[0]}.</h1>
      <p>Your newest lessons, resources, and assessments are ready.</p></div><i className="bi bi-mortarboard" /></div>
    <div className="d-flex justify-content-between align-items-end my-4"><div><h3>Latest content</h3>
      <p className="text-secondary mb-0">Published newest first</p></div>
      <span className="badge text-bg-light">{items.length} items</span></div>
    {error && <div className="alert alert-danger" role="alert">{error}</div>}
    <div className="content-grid">{items.map(item => <button type="button" className="content-card content-card-button" key={item.id} onClick={() => open(item)}>
      <div className={'type-icon ' + item.type.toLowerCase()}><i className={'bi ' + icons[item.type]} /></div>
      <div><div className="content-meta"><span>{item.type}</span><time>{new Date(item.uploadedAt).toLocaleDateString()}</time></div>
        <h4>{item.title}</h4><p>{item.body}</p>
        {item.type === 'IMAGE' && item.fileAvailable && <img className="content-thumbnail" loading="lazy" src={apiUrl(`/contents/${item.id}/file`)} alt={item.title} />}
        <div className="content-footer"><span><i className="bi bi-person-circle" /> {item.author.name}</span>
          {item.type === 'QUIZ' && <span><i className="bi bi-clock" /> {item.quizDurationMinutes} min · {item.quizAttemptCount} students · Best {item.quizMaxScore}</span>}
          <b>Open <i className="bi bi-arrow-right" /></b></div></div>
    </button>)}</div>
    {selected && <div className="modal-backdrop-custom" onMouseDown={e => e.target === e.currentTarget && !takingQuiz && close()}>
      {takingQuiz && quiz ? <QuizComponent quiz={quiz} session={session} close={close} onComplete={completed} /> :
        <ContentDetails item={selected} close={close}>
          {selected.type === 'QUIZ' && <div className="quiz-overview mt-3">
            {quiz ? <><div className="d-flex gap-3 flex-wrap mb-3">
              <span>{quiz.durationMinutes} minutes</span><span>{quiz.questionCount} questions</span>
              <span>{quiz.attemptCount} students attempted</span><span>Best score {quiz.maxScore}</span></div>
              {session.user.role === 'STUDENT' && <button className="btn btn-primary" onClick={() => setTakingQuiz(true)}>Start quiz</button>}
            </> : <span>Loading quiz details…</span>}
          </div>}
        </ContentDetails>}
    </div>}
  </>;
}
