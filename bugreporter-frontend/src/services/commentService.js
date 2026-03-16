import api from "./api";

export const getCommentsForBug = (bugId) => {
    return api.get(`/comments/bug/${bugId}`);
}

export const createComment = (bugId, payload) => {
    return api.post(`/comments/bug/${bugId}`, payload)
}

export const deleteComment = (commentId) => {
    return api.delete(`/comments/${commentId}`);
}

export const voteComment = (commentId, voteType) => {
    return api.post(`/comments/${commentId}/vote`, {voteType});
}