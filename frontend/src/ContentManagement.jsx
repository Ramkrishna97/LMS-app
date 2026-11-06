import React, { useEffect, useState } from 'react';
import { apiUrl } from './api.js';

const emptyFilters = { q: '', authorId: '', type: '', from: '', to: '' };

export default function ContentManagement({ session, onAuthExpired = () => {}, ownOnly = false }) {
  const [filters, setFilters] = useState(emptyFilters);
  const [applied, setApplied] = useState(emptyFilters);
  const [page, setPage] = useState(0);
  const [reload, setReload] = useState(0);
  const [result, setResult] = useState({ content: [], totalElements: 0, totalPages: 0 });
  const [authors, setAuthors] = useState([]);
  const [editing, setEditing] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const api = async (path, options = {}) => {
    const response = await fetch(apiUrl(path), {
      ...options,
      headers: { 'X-Auth-Token': session.token, ...(options.body ? { 'Content-Type': 'application/json' } : {}) }
    });
    const data = await response.json().catch(() => null);
    if (!response.ok) {
      if (data?.message === 'Please sign in') onAuthExpired();
      throw new Error(data?.message || `Request failed (${response.status})`);
    }
    return data;
  };

  useEffect(() => {
    if (ownOnly) return;
    api('/users').then(users => setAuthors(users.filter(user => user.role === 'TEACHER' || user.role === 'ADMIN')))
      .catch(exception => setError(exception.message));
  }, [ownOnly]);

  useEffect(() => {
    const params = new URLSearchParams({ page: String(page), size: '20' });
    Object.entries(applied).forEach(([key, value]) => { if (value) params.set(key, value); });
    const endpoint = ownOnly ? '/contents/mine?' : '/contents/manage?';
    api(endpoint + params).then(setResult).catch(exception => setError(exception.message));
  }, [applied, page, reload, ownOnly]);

  const filter = event => { event.preventDefault(); setPage(0); setApplied({ ...filters }); };
  const reset = () => { setFilters(emptyFilters); setApplied(emptyFilters); setPage(0); };
  const save = async event => {
    event.preventDefault(); setBusy(true); setError('');
    try {
      await api(`/contents/${editing.id}`, { method: 'PUT', body: JSON.stringify({
        title: editing.title, body: editing.body, type: editing.type
      }) });
      setEditing(null); setReload(value => value + 1);
    } catch (exception) { setError(exception.message); }
    finally { setBusy(false); }
  };
  const remove = async item => {
    if (!confirm(`Delete “${item.title}” and its associated quiz attempts, if any?`)) return;
    setBusy(true); setError('');
    try { await api(`/contents/${item.id}`, { method: 'DELETE' });
      if (result.content.length === 1 && page > 0) setPage(page - 1);
      else setReload(value => value + 1);
    } catch (exception) { setError(exception.message); }
    finally { setBusy(false); }
  };

  return <div className="content-management">
    <div className="mb-4"><h2>{ownOnly ? 'My published content' : 'Content management'}</h2>
      <p className="text-secondary">{ownOnly
        ? 'Review, filter, edit, or remove anything you have published.'
        : 'Find, edit, or remove published content. Filters are applied across all pages.'}</p></div>
    {error && <div className="alert alert-danger" role="alert">{error}</div>}
    <form className={`management-filters ${ownOnly ? 'own-content-filters' : ''}`} onSubmit={filter}>
      <div><label htmlFor="content-search">Title</label><input id="content-search" className="form-control"
        value={filters.q} onChange={event => setFilters({ ...filters, q: event.target.value })} placeholder="Search title" /></div>
      {!ownOnly && <div><label htmlFor="content-author">Posted by</label><select id="content-author" className="form-select"
        value={filters.authorId} onChange={event => setFilters({ ...filters, authorId: event.target.value })}>
        <option value="">All authors</option>{authors.map(author => <option key={author.id} value={author.id}>{author.name}</option>)}
      </select></div>}
      <div><label htmlFor="content-filter-type">Type</label><select id="content-filter-type" className="form-select"
        value={filters.type} onChange={event => setFilters({ ...filters, type: event.target.value })}>
        <option value="">All types</option>{['POST', 'DOCUMENT', 'IMAGE', 'QUIZ'].map(type => <option key={type}>{type}</option>)}
      </select></div>
      <div><label htmlFor="content-from">From</label><input id="content-from" className="form-control" type="date"
        value={filters.from} onChange={event => setFilters({ ...filters, from: event.target.value })} /></div>
      <div><label htmlFor="content-to">To</label><input id="content-to" className="form-control" type="date"
        value={filters.to} onChange={event => setFilters({ ...filters, to: event.target.value })} /></div>
      <div className="filter-actions"><button className="btn btn-primary">Apply</button>
        <button className="btn btn-outline-secondary" type="button" onClick={reset}>Reset</button></div>
    </form>
    <div className="table-responsive bg-white rounded-4 shadow-sm">
      <table className="table align-middle mb-0"><thead><tr><th>Content</th><th>Type</th>{!ownOnly && <th>Teacher / author</th>}<th>Uploaded</th><th>Actions</th></tr></thead>
        <tbody>{result.content.map(item => <tr key={item.id}>
          <td><b>{item.title}</b><small className="d-block text-secondary content-management-description">{item.body}</small></td>
          <td>{item.type}</td>{!ownOnly && <td>{item.author.name}</td>}<td>{new Date(item.uploadedAt).toLocaleString()}</td>
          <td><div className="d-flex gap-2"><button className="btn btn-sm btn-outline-primary" onClick={() => setEditing({ ...item })}>Edit</button>
            <button className="btn btn-sm btn-outline-danger" disabled={busy} onClick={() => remove(item)}>Delete</button></div></td>
        </tr>)}</tbody>
      </table>
      {result.content.length === 0 && <p className="text-secondary text-center py-4 mb-0">No content matches these filters.</p>}
    </div>
    <div className="d-flex justify-content-between align-items-center mt-3">
      <span className="text-secondary">{result.totalElements} items · Page {page + 1} of {Math.max(1, result.totalPages)}</span>
      <div className="d-flex gap-2"><button className="btn btn-outline-primary" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
        <button className="btn btn-outline-primary" disabled={page + 1 >= result.totalPages} onClick={() => setPage(page + 1)}>Next</button></div>
    </div>
    {editing && <div className="modal-backdrop-custom" onMouseDown={event => event.target === event.currentTarget && setEditing(null)}>
      <form className="dialog" onSubmit={save} role="dialog" aria-modal="true" aria-label="Edit content">
        <button className="dialog-close" type="button" onClick={() => setEditing(null)} aria-label="Close">×</button>
        <h3>Edit {editing.type.toLowerCase()}</h3>
        <p className="text-secondary">Edit the homepage title and description. Uploaded files and quiz questions remain unchanged.</p>
        <label className="form-label" htmlFor="edit-content-title">Title</label>
        <input id="edit-content-title" className="form-control mb-3" required value={editing.title}
          onChange={event => setEditing({ ...editing, title: event.target.value })} />
        <label className="form-label" htmlFor="edit-content-body">Description / text</label>
        <textarea id="edit-content-body" className="form-control mb-3" rows="5" value={editing.body || ''}
          onChange={event => setEditing({ ...editing, body: event.target.value })} />
        <button className="btn btn-primary" disabled={busy}>Save changes</button>
      </form>
    </div>}
  </div>;
}
