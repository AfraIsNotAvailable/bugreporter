import api from "../api/axios";

export const getBugs = () => api.get("/bugs");

export const filterBugs = (params) => api.get("/bugs/filter", { params });

export const getBug = (id) => api.get(`/bugs/${id}`);

export const createBug = (payload) => api.post("/bugs", payload);

export const updateBug = (id, payload) => api.put(`/bugs/${id}`, payload);

export const deleteBug = (id) => api.delete(`/bugs/${id}`);

export const addBugTags = (id, tags) => api.post(`/bugs/${id}/tags`, tags);

export const resolveBug = (id) => api.patch(`/bugs/${id}/resolve`);

export const updateBugStatus = (id, status) =>
  api.patch(`/bugs/${id}/status`, null, { params: { status } });
