const configuredBase = import.meta.env.VITE_API_BASE_URL || '';

// Local development uses the Spring Boot origin. Netlify builds set VITE_API_BASE_URL
// to the public Render service URL, without a trailing slash.
export const API_BASE_URL = configuredBase.replace(/\/$/, '');

export const apiUrl = path => `${API_BASE_URL}/api${path}`;
