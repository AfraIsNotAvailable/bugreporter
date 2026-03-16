import {useEffect, useState} from "react";
import {getCommentsForBug} from "../services/commentService";

export function useComments(bugId) {
    const [comments, setComments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        getCommentsForBug(bugId)
            .then(response => setComments(response.data))
            .catch(err => setError(err))
            .finally(() => setLoading(false));
    }, [bugId]);

    return {comments, setComments, loading, error};
}