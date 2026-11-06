# Deploy LearnHub to Netlify and Render

The deployment has two services:

| Service | Host | Purpose |
| --- | --- | --- |
| React frontend | Netlify | The public learning-platform UI |
| Spring Boot API | Render | APIs, uploaded files, and the H2 database |

## 1. Push this project to GitHub

Both hosts deploy from a Git repository. Commit the project, including [netlify.toml](./netlify.toml) and [render.yaml](./render.yaml), then push it to GitHub.

## 2. Deploy the API on Render

1. In Render, choose **New > Blueprint** and select the repository.
2. Render reads [render.yaml](./render.yaml) and builds the Java 17 backend with [Dockerfile](./Dockerfile). Keep the paid `0.5c-512mb` plan: the attached persistent disk is required for the H2 database and files that users upload.
3. Create the service and wait for the health check at `/api/health` to pass.
4. Copy its HTTPS URL, for example `https://learnhub-api.onrender.com`.
5. In the service environment variables, set `APP_CORS_ALLOWED_ORIGINS` to the precise Netlify URL after the frontend is created, for example `https://learnhub.netlify.app`. Include a custom domain too if you add one, separated with commas.

The supplied configuration stores H2 under `/var/data/lms`, which is the persistent-disk mount. Do not use a free Render web service with H2: its filesystem is erased whenever it restarts, sleeps, or redeploys. Render documents that persistent disks require a paid web service. For a free backend, replace H2 with a managed database before deployment.

## 3. Deploy the frontend on Netlify

1. In Netlify, choose **Add new site > Import an existing project**, then select the same repository.
2. Netlify reads [netlify.toml](./netlify.toml). It builds from `frontend` and publishes `frontend/dist`.
3. In **Site configuration > Environment variables**, create:

   ```text
   VITE_API_BASE_URL=https://your-render-service.onrender.com
   ```

   Use the Render URL exactly, with no trailing `/`. This value is intentionally public; it is the browser-visible API address, not a secret.
4. Deploy the site, then copy the Netlify URL back into Render’s `APP_CORS_ALLOWED_ORIGINS` value and redeploy the API.
5. Trigger a final Netlify deploy after changing `VITE_API_BASE_URL`.

## Local development

Use [start.bat](./start.bat) to run the combined local application. Without `VITE_API_BASE_URL`, React calls the same Spring Boot origin at `/api`.

Run [build-deployment.bat](./build-deployment.bat) before pushing when you want to reproduce the backend and frontend deployment builds locally. If `VITE_API_BASE_URL` is defined in your terminal, Vite embeds that URL in the generated frontend bundle.

## Deployment checks

After both deploys finish:

1. Open `https://your-render-service.onrender.com/api/health` and confirm it returns `{"status":"ok"}`.
2. Open the Netlify site and confirm the homepage content loads.
3. Sign in, publish a small post, and reload the site.
4. Restart/redeploy the Render service and confirm the post is still present. This verifies the persistent H2 disk is attached.
