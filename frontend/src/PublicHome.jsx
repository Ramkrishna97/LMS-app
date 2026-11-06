import React, { useEffect, useState } from 'react';
import ContentDetails from './ContentDetails.jsx';
import { apiUrl } from './api.js';

const icon = { POST: 'bi-file-text', QUIZ: 'bi-patch-question', DOCUMENT: 'bi-file-earmark-pdf', IMAGE: 'bi-image' };

export default function PublicHome({ onSignIn }) {
  const [items, setItems] = useState([]);
  const [detail, setDetail] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => {
    fetch(apiUrl('/dashboard')).then(r => {
      if (!r.ok) throw new Error('Could not load the learning feed.');
      return r.json();
    }).then(setItems).catch(e => setError(e.message));
  }, []);
  return <div className="public-page">
    <nav className="public-nav">
      <div className="sidebar-brand"><span>LH</span><b>LearnHub</b></div>
      <button className="btn btn-primary" onClick={onSignIn}><i className="bi bi-box-arrow-in-right" /> Sign in</button>
    </nav>
    <section className="public-hero">
      <div><span className="eyebrow">LEARN WITHOUT LIMITS</span>
        <h1>Knowledge shared.<br />Progress made.</h1>
        <p>Browse published lessons, documents, images, and quizzes. Sign in when you want to participate or publish.</p>
        <button className="btn btn-light btn-lg" onClick={onSignIn}>Sign in or create account</button>
      </div><i className="bi bi-journal-richtext" />
    </section>
    <main className="public-content">
      <div className="d-flex justify-content-between align-items-end mb-4">
        <div><h2>Latest learning content</h2><p className="text-secondary mb-0">Available publicly, newest first</p></div>
        <span className="badge text-bg-light">{items.length} published</span>
      </div>
      {error && <div className="alert alert-danger">{error}</div>}
      <div className="content-grid">{items.map(x => <button type="button" className="content-card content-card-button" key={x.id} onClick={() => setDetail(x)}>
        <div className={'type-icon ' + x.type.toLowerCase()}><i className={'bi ' + icon[x.type]} /></div>
        <div><div className="content-meta"><span>{x.type}</span><time>{new Date(x.uploadedAt).toLocaleDateString()}</time></div>
          <h4>{x.title}</h4><p>{x.body}</p>
          {x.type === 'IMAGE' && x.fileAvailable && <img className="content-thumbnail" loading="lazy" src={apiUrl('/contents/' + x.id + '/file')} alt={x.title} />}
          <div className="content-footer"><span><i className="bi bi-person-circle" /> {x.author.name}</span>
            {x.type === 'QUIZ' && <span><i className="bi bi-clock" /> {x.quizDurationMinutes} min · {x.quizAttemptCount} students · Best {x.quizMaxScore}</span>}
            <b>View <i className="bi bi-arrow-right" /></b></div>
        </div>
      </button>)}</div>
    </main>
    {detail && <div className="modal-backdrop-custom" onMouseDown={e => e.target === e.currentTarget && setDetail(null)}>
      <ContentDetails item={detail} close={() => setDetail(null)}>
        {detail.type === 'QUIZ' && <div className="alert alert-info mt-3">{detail.quizQuestionCount} questions · {detail.quizDurationMinutes} minutes · {detail.quizAttemptCount} students attempted · Best score {detail.quizMaxScore}. Sign in as a student to attempt this quiz.</div>}
      </ContentDetails>
    </div>}
  </div>;
}
