import React from 'react';

export const blankQuestion = () => ({
  questionText: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: '', solution: ''
});

export default function QuizBuilder({ quiz, setQuiz }) {
  const change = (key, value) => setQuiz(previous => ({ ...previous, [key]: value }));
  const changeQuestion = (index, key, value) => setQuiz(previous => ({
    ...previous,
    questions: previous.questions.map((question, number) =>
      number === index ? { ...question, [key]: value } : question)
  }));
  return <div className="quiz-builder">
    <h3>Build a quiz</h3>
    <p className="text-secondary">Add a question, four choices, the correct choice, and its explanation.</p>
    <label className="form-label" htmlFor="quiz-title">Quiz name</label>
    <input id="quiz-title" className="form-control mb-3" required value={quiz.title}
      onChange={event => change('title', event.target.value)} />
    <label className="form-label" htmlFor="quiz-description">Description</label>
    <textarea id="quiz-description" className="form-control mb-3" required rows="3" value={quiz.description}
      onChange={event => change('description', event.target.value)} />
    <label className="form-label" htmlFor="quiz-duration">Time limit (minutes)</label>
    <input id="quiz-duration" className="form-control mb-3" type="number" min="1" required value={quiz.durationMinutes}
      onChange={event => change('durationMinutes', event.target.value)} />
    {quiz.questions.map((question, index) => <section className="quiz-question-editor" key={index}>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h4 className="h5 mb-0">Question {index + 1}</h4>
        {quiz.questions.length > 1 && <button className="btn btn-sm btn-outline-danger" type="button"
          onClick={() => setQuiz(previous => ({ ...previous, questions: previous.questions.filter((_, i) => i !== index) }))}>
          Remove
        </button>}
      </div>
      <label className="form-label" htmlFor={`question-${index}`}>Question text</label>
      <textarea id={`question-${index}`} className="form-control mb-3" rows="2" required
        value={question.questionText} onChange={event => changeQuestion(index, 'questionText', event.target.value)} />
      <div className="row">
        {['optionA', 'optionB', 'optionC', 'optionD'].map((option, choice) => <div className="col-md-6" key={option}>
          <label className="form-label" htmlFor={`question-${index}-${option}`}>Option {choice + 1}</label>
          <input id={`question-${index}-${option}`} className="form-control mb-3" required value={question[option]}
            onChange={event => changeQuestion(index, option, event.target.value)} />
        </div>)}
      </div>
      <label className="form-label" htmlFor={`correct-${index}`}>Correct option</label>
      <select id={`correct-${index}`} className="form-select mb-3" required value={question.correctOption}
        onChange={event => changeQuestion(index, 'correctOption', event.target.value)}>
        <option value="">Choose the correct option</option>
        {[0, 1, 2, 3].map(choice => <option value={choice} key={choice}>Option {choice + 1}</option>)}
      </select>
      <label className="form-label" htmlFor={`solution-${index}`}>Solution / explanation</label>
      <textarea id={`solution-${index}`} className="form-control" rows="2" required value={question.solution}
        onChange={event => changeQuestion(index, 'solution', event.target.value)} />
    </section>)}
    <button className="btn btn-outline-primary mt-3" type="button"
      onClick={() => setQuiz(previous => ({ ...previous, questions: [...previous.questions, blankQuestion()] }))}>
      <i className="bi bi-plus-circle" /> Add question
    </button>
  </div>;
}
