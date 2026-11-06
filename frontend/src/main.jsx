import React,{useEffect,useState} from 'react';
import {createRoot} from 'react-dom/client';
import 'bootstrap/dist/css/bootstrap.min.css';
import 'bootstrap-icons/font/bootstrap-icons.css';
import './styles.css';
import './public.css';
import './crop.css';
import './motion.css';
import './management.css';
import PublicHome from './PublicHome.jsx';
import UploadPublisher from './UploadPublisher.jsx';
import AuthenticatedDashboard from './AuthenticatedDashboard.jsx';
import ContentManagement from './ContentManagement.jsx';
import ImageCropper from './ImageCropper.jsx';
import { apiUrl } from './api.js';

const request=async(path,options={},token)=>{const r=await fetch(apiUrl(path),{...options,headers:{...(options.body instanceof FormData?{}:{'Content-Type':'application/json'}),...(token?{'X-Auth-Token':token}:{}),...(options.headers||{})}});const data=await r.json().catch(()=>null);if(!r.ok)throw new Error(data?.message||'Request failed');return data};

function Auth({onLogin}){const[mode,setMode]=useState('login'),[form,setForm]=useState({role:'STUDENT'}),[message,setMessage]=useState('');const field=(key,value)=>setForm({...form,[key]:value});const submit=async e=>{e.preventDefault();try{if(mode==='forgot'){const x=await request('/auth/forgot-password',{method:'POST',body:JSON.stringify(form)});return setMessage('Temporary password: '+x.temporaryPassword)}if(mode==='register'){await request('/auth/register',{method:'POST',body:JSON.stringify(form)});setMessage('Account created. Sign in now.');return setMode('login')}onLogin(await request('/auth/login',{method:'POST',body:JSON.stringify(form)}))}catch(err){setMessage(err.message)}};return <div className="auth-page"><div className="auth-brand"><div className="brand-mark">LH</div><h1>LearnHub</h1><p>A focused space for teaching, learning, and growth.</p></div><form className="card border-0 shadow-lg auth-card" onSubmit={submit}><div className="card-body p-4 p-lg-5"><h2>{mode==='login'?'Welcome back':mode==='register'?'Create your account':'Forgot password'}</h2><p className="text-secondary">{mode==='login'?'Sign in to continue to your dashboard.':mode==='register'?'Choose your learning role and get started.':'We will generate a temporary password.'}</p>{message&&<div className="alert alert-info">{message}</div>}{mode==='register'&&<><label className="form-label">Full name</label><input className="form-control mb-3" required onChange={e=>field('name',e.target.value)}/><label className="form-label">Email</label><input className="form-control mb-3" type="email" required onChange={e=>field('email',e.target.value)}/><label className="form-label">Role</label><select className="form-select mb-3" onChange={e=>field('role',e.target.value)}><option value="STUDENT">Student</option><option value="TEACHER">Teacher</option></select></>}<label className="form-label">{mode==='forgot'?'Email':'Username'}</label><input className="form-control mb-3" required onChange={e=>field(mode==='forgot'?'email':'username',e.target.value)}/>{mode!=='forgot'&&<><label className="form-label">Password</label><input className="form-control mb-3" type="password" required onChange={e=>field('password',e.target.value)}/></>}<button className="btn btn-primary w-100 py-2">{mode==='login'?'Sign in':mode==='register'?'Create account':'Generate temporary password'}</button><div className="d-flex justify-content-between mt-4"><button type="button" className="btn btn-link p-0" onClick={()=>setMode(mode==='login'?'register':'login')}>{mode==='login'?'Create account':'Back to sign in'}</button>{mode==='login'&&<button type="button" className="btn btn-link p-0" onClick={()=>setMode('forgot')}>Forgot password?</button>}</div></div></form></div>}

function Shell({session,setSession,onAuthExpired}){
  const {token,user}=session;
  const [page,setPage]=useState('dashboard');
  const [menuOpen,setMenuOpen]=useState(false);
  const nav=[['dashboard','bi-grid','Dashboard'],
    ...(user.role!=='STUDENT'?[['publish','bi-plus-square','Publish content']]:[]),
    ...(user.role==='ADMIN'?[['users','bi-people','User management'],['contents','bi-collection','Content management']]:[])];
  const logout=async()=>{try{await fetch(apiUrl('/auth/logout'),{method:'POST',headers:{'X-Auth-Token':token}})}catch{}
    localStorage.removeItem('lms-session');setSession(null)};
  const updateUser=updated=>{const next={...session,user:updated};localStorage.setItem('lms-session',JSON.stringify(next));setSession(next)};
  return <div className="app-shell"><aside>
    <div className="sidebar-brand"><span>LH</span><b>LearnHub</b></div>
    <nav>{nav.map(([id,icon,label])=><button key={id} className={page===id?'active':''}
      onClick={()=>{setPage(id);setMenuOpen(false)}}><i className={'bi '+icon}/>{label}</button>)}</nav>
  </aside><section className="workspace"><header><div><small>LEARNHUB</small>
    <h4>{page==='profile'?'My Profile':nav.find(item=>item[0]===page)?.[2]}</h4></div>
    <div className="profile-menu-wrap"><button className="profile-trigger" type="button" aria-label="Open profile menu"
      aria-expanded={menuOpen} onClick={()=>setMenuOpen(open=>!open)}>
      <img src={apiUrl(`/users/${user.id}/photo?v=${user.photoVersion||''}`)} alt="" onError={event=>{event.currentTarget.onerror=null;event.currentTarget.src='/avatar-placeholder.svg'}} />
    </button>{menuOpen&&<div className="profile-menu" role="menu">
      <div className="profile-menu-identity"><b>{user.name}</b><small>{user.role}</small></div>
      <button role="menuitem" onClick={()=>{setPage('profile');setMenuOpen(false)}}><i className="bi bi-person" /> My Profile</button>
      <button role="menuitem" onClick={logout}><i className="bi bi-box-arrow-left" /> Sign out</button>
    </div>}</div></header><main>
    {page==='dashboard'&&<AuthenticatedDashboard session={session} QuizComponent={Quiz}/>}
    {page==='publish'&&<UploadPublisher session={session} done={()=>setPage('dashboard')} onAuthExpired={onAuthExpired}/>}
    {page==='users'&&<Users session={session}/>}
    {page==='contents'&&<ContentManagement session={session} onAuthExpired={onAuthExpired}/>}
    {page==='profile'&&<Profile session={session} update={updateUser} onAuthExpired={onAuthExpired}/>} 
  </main></section></div>
}



function Users({session}){
  const [users,setUsers]=useState([]),[show,setShow]=useState(false);
  const [editing,setEditing]=useState(null),[form,setForm]=useState({role:'STUDENT'}),[message,setMessage]=useState('');
  const load=()=>request('/users',{},session.token).then(setUsers).catch(error=>setMessage(error.message));
  useEffect(()=>{load()},[]);
  const add=async event=>{event.preventDefault();try{
    await request('/users',{method:'POST',body:JSON.stringify(form)},session.token);
    setShow(false);setMessage('');load();
  }catch(error){setMessage(error.message)}};
  const save=async event=>{event.preventDefault();try{
    await request(`/users/${editing.id}`,{method:'PUT',body:JSON.stringify(editing)},session.token);
    setEditing(null);setMessage('');load();
  }catch(error){setMessage(error.message)}};
  const remove=async user=>{
    const warning=user.role==='TEACHER'?' Their published content and associated quiz attempts will also be deleted.':
      ' Their quiz attempts will also be deleted.';
    if(!confirm(`Delete ${user.name}?${warning}`))return;
    try{await request(`/users/${user.id}`,{method:'DELETE'},session.token);setMessage('');load()}
    catch(error){setMessage(error.message)}
  };
  return <><div className="d-flex justify-content-between mb-4"><div><h2>User management</h2>
    <p className="text-secondary">Add, edit, or remove student and teacher accounts.</p></div>
    <button className="btn btn-primary" onClick={()=>{setMessage('');setShow(true)}}><i className="bi bi-person-plus"/> Add user</button></div>
    {message&&<div className="alert alert-danger">{message}</div>}
    <div className="table-responsive bg-white rounded-4 shadow-sm"><table className="table align-middle mb-0">
      <thead><tr><th>User</th><th>Username</th><th>Role</th><th>Created</th><th>Actions</th></tr></thead>
      <tbody>{users.map(user=><tr key={user.id}><td><b>{user.name}</b>
        <small className="d-block text-secondary">{user.email}</small></td><td>{user.username||'—'}</td>
        <td>{user.role}</td><td>{new Date(user.createdAt).toLocaleDateString()}</td>
        <td>{user.role!=='ADMIN'&&<div className="d-flex gap-2"><button className="btn btn-sm btn-outline-primary"
          onClick={()=>{setMessage('');setEditing({...user})}}>Edit</button>
          <button className="btn btn-sm btn-outline-danger" onClick={()=>remove(user)}>Delete</button></div>}</td>
      </tr>)}</tbody></table></div>
    {show&&<div className="modal-backdrop-custom"><form className="dialog" onSubmit={add}>
      <button type="button" className="dialog-close" onClick={()=>setShow(false)} aria-label="Close">×</button>
      <h3>Add user</h3>{message&&<div className="alert alert-danger">{message}</div>}
      {['name','email','username','password'].map(key=><input key={key} className="form-control mb-3"
        type={key==='password'?'password':'text'} placeholder={key[0].toUpperCase()+key.slice(1)} required
        onChange={event=>setForm({...form,[key]:event.target.value})}/>)}
      <select className="form-select mb-3" onChange={event=>setForm({...form,role:event.target.value})}>
        <option value="STUDENT">Student</option><option value="TEACHER">Teacher</option></select>
      <button className="btn btn-primary">Create user</button></form></div>}
    {editing&&<div className="modal-backdrop-custom"><form className="dialog" onSubmit={save}>
      <button type="button" className="dialog-close" onClick={()=>setEditing(null)} aria-label="Close">×</button>
      <h3>Edit {editing.name}</h3>{message&&<div className="alert alert-danger">{message}</div>}
      <label className="form-label">Name</label><input className="form-control mb-3" required value={editing.name||''}
        onChange={event=>setEditing({...editing,name:event.target.value})}/>
      <label className="form-label">Age</label><input className="form-control mb-3" type="number" value={editing.age||''}
        onChange={event=>setEditing({...editing,age:Number(event.target.value)})}/>
      <label className="form-label">Phone</label><input className="form-control mb-3" value={editing.phone||''}
        onChange={event=>setEditing({...editing,phone:event.target.value})}/>
      <label className="form-label">Bio</label><textarea className="form-control mb-3" value={editing.bio||''}
        onChange={event=>setEditing({...editing,bio:event.target.value})}/>
      <button className="btn btn-primary">Save changes</button></form></div>}
  </>;
}

function Profile({session,update,onAuthExpired}){
  const [user,setUser]=useState(session.user);
  const [password,setPassword]=useState('');
  const [notice,setNotice]=useState('');
  const [cropSource,setCropSource]=useState(null);
  const [sourceName,setSourceName]=useState('');
  useEffect(()=>()=>{if(cropSource)URL.revokeObjectURL(cropSource)},[cropSource]);
  const save=async event=>{event.preventDefault();try{
    const updated=await request('/users/me',{method:'PUT',body:JSON.stringify(user)},session.token);
    if(password)await request('/auth/password',{method:'POST',body:JSON.stringify({password})},session.token);
    const next={...updated,photoVersion:user.photoVersion};setUser(next);update(next);setPassword('');setNotice('Profile updated.');
  }catch(error){setNotice(error.message)}};
  const selectPhoto=event=>{const file=event.target.files?.[0];event.target.value='';if(!file)return;
    setSourceName(file.name);setNotice('');setCropSource(URL.createObjectURL(file))};
  const uploadPhoto=async file=>{const data=new FormData();data.append('file',file);
    const updated=await request('/users/me/photo',{method:'POST',body:data},session.token);
    const next={...updated,photoVersion:Date.now()};setUser(next);update(next);setCropSource(null);
    setNotice('Profile photo updated.');
  };
  return <div className="profile-page"><form className="form-panel" onSubmit={save}><h2>Profile & account</h2>
    <p className="text-secondary">Update your personal information and profile picture.</p>
    {notice&&<div className="alert alert-info">{notice}</div>}
    <div className="photo-row"><img src={apiUrl(`/users/${user.id}/photo?v=${user.photoVersion||''}`)} alt="Profile"
      onError={event=>{event.currentTarget.onerror=null;event.currentTarget.src='/avatar-placeholder.svg'}}/>
      <div><label className="btn btn-outline-primary">Choose and crop profile photo
        <input hidden type="file" accept="image/*" onChange={selectPhoto}/></label>
        <small>Choose the square crop and zoom. The final photo is stored below 50 KB.</small></div></div>
    <label className="form-label">Name</label><input className="form-control mb-3" value={user.name||''}
      onChange={event=>setUser({...user,name:event.target.value})}/>
    <div className="row"><div className="col-md-4"><label className="form-label">Age</label>
      <input className="form-control mb-3" type="number" value={user.age||''}
        onChange={event=>setUser({...user,age:+event.target.value})}/></div>
      <div className="col-md-8"><label className="form-label">Phone</label>
        <input className="form-control mb-3" value={user.phone||''}
          onChange={event=>setUser({...user,phone:event.target.value})}/></div></div>
    <label className="form-label">Bio</label><textarea className="form-control mb-3" rows="3" value={user.bio||''}
      onChange={event=>setUser({...user,bio:event.target.value})}/>
    <label className="form-label">New password</label><input className="form-control mb-3" type="password"
      value={password} placeholder="Leave blank to keep current password"
      onChange={event=>setPassword(event.target.value)}/>
    <button className="btn btn-primary">Save changes</button>
  </form>{session.user.role==='TEACHER'&&<ContentManagement session={session} onAuthExpired={onAuthExpired} ownOnly/>}
    {cropSource&&<ImageCropper source={cropSource} name={sourceName} aspect={1} maxDimension={160}
      onCancel={()=>setCropSource(null)} onDone={uploadPhoto}/>}</div>
}

function Quiz({quiz,session,close,onComplete}){const[answers,setAnswers]=useState({}),[result,setResult]=useState(null);const submit=async()=>{const completed=await request('/quizzes/'+quiz.id+'/attempt',{method:'POST',body:JSON.stringify({answers})},session.token);setResult(completed);onComplete?.(completed)};return <div className="dialog quiz-dialog"><button className="dialog-close" onClick={()=>result?close():alert('Submit to leave')}>×</button>{result?<><span className="badge text-bg-success">COMPLETED</span><h2 className="mt-3">Score: {result.score}/{result.total}</h2>{result.solutions.map((s,i)=><div className="solution" key={i}><b>{s.question}</b><div>Correct option: {s.correctOption+1}</div><small>{s.solution}</small></div>)}<button className="btn btn-primary" onClick={()=>{setAnswers({});setResult(null)}}>Re-attempt</button></>:<><span className="badge text-bg-warning">TIMED QUIZ</span><h2 className="mt-3">{quiz.title}</h2><p>{quiz.durationMinutes} minutes · {quiz.attemptCount} students attempted · Best score {quiz.maxScore}</p>{quiz.questions.map((q,i)=><fieldset key={q.id}><legend>{i+1}. {q.questionText}</legend>{[q.optionA,q.optionB,q.optionC,q.optionD].map((v,n)=><label key={n}><input type="radio" name={q.id} checked={answers[q.id]===n} onChange={()=>setAnswers({...answers,[q.id]:n})}/>{v}</label>)}</fieldset>)}<button className="btn btn-primary" onClick={submit}>Submit quiz</button></>}</div>}

function App(){
  const [session,setSession]=useState(()=>{try{return JSON.parse(localStorage.getItem('lms-session')||'null')}catch{return null}});
  const [showLogin,setShowLogin]=useState(false);
  const [checking,setChecking]=useState(!!session);
  useEffect(()=>{
    if(!session?.token){setChecking(false);return}
    fetch(apiUrl('/auth/me'),{headers:{'X-Auth-Token':session.token}}).then(async response=>{
      if(!response.ok)throw new Error('Session expired');
      const user=await response.json();
      setSession(current=>{if(!current||current.token!==session.token)return current;
        const updated={...current,user};localStorage.setItem('lms-session',JSON.stringify(updated));return updated});
    }).catch(()=>{localStorage.removeItem('lms-session');setSession(null);setShowLogin(true)})
      .finally(()=>setChecking(false));
  },[]);
  const login=next=>{localStorage.setItem('lms-session',JSON.stringify(next));setSession(next)};
  const expired=()=>{localStorage.removeItem('lms-session');setSession(null);setShowLogin(true)};
  if(checking)return <div className="session-check"><span className="spinner-border text-primary" role="status"/><p>Checking your session…</p></div>;
  return session?<Shell session={session} setSession={setSession} onAuthExpired={expired}/>:
    showLogin?<Auth onLogin={login}/>:<PublicHome onSignIn={()=>setShowLogin(true)}/>;
}
createRoot(document.getElementById('root')).render(<App/>);
