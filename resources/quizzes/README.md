# LearnHub quiz JSON samples

Upload any JSON file in this directory from **Publish content > QUIZ > Import JSON**.

The format is defined by [schema/quiz.schema.json](./schema/quiz.schema.json). `correctOption` is zero-based:

| Value | Correct answer |
| --- | --- |
| `0` | `optionA` |
| `1` | `optionB` |
| `2` | `optionC` |
| `3` | `optionD` |

To create another quiz, copy one of the samples, change its details and questions, and keep every required field. The application rejects blank fields, durations below one minute, questions without four options, and `correctOption` values outside `0` to `3`.
