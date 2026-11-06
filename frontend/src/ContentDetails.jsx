import React from 'react';
import { apiUrl } from './api.js';

export default function ContentDetails({ item, close, children }) {
  const fileUrl = apiUrl(`/contents/${item.id}/file`);
  const isPdf = item.fileName?.toLowerCase().endsWith('.pdf');
  return <div className="dialog content-dialog" role="dialog" aria-modal="true" aria-label={item.title}>
    <button className="dialog-close" onClick={close} aria-label="Close">×</button>
    <span className="badge text-bg-primary">{item.type}</span>
    <h2 className="mt-3">{item.title}</h2>
    <p>{item.body}</p>
    {item.type === 'IMAGE' && item.fileAvailable && <img className="detail-image" src={fileUrl} alt={item.title} />}
    {item.type === 'DOCUMENT' && item.fileAvailable && <>
      {isPdf && <iframe className="document-frame" title={item.title} src={fileUrl} />}
      <a className="btn btn-outline-primary mt-3" href={fileUrl} download={item.fileName}>Download {item.fileName}</a>
    </>}
    {children}
    <p className="text-secondary mt-3 mb-0 small">Uploaded by {item.author.name} · {new Date(item.uploadedAt).toLocaleString()}</p>
  </div>;
}
