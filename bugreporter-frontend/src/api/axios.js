import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8081",
});

//inainte de fiecare request catre backend, adaug tokenul in header daca exista
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  config.headers = config.headers ?? {};


  //verific daca token-ul exista si il adaug in header la Authorization
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

export default api;
