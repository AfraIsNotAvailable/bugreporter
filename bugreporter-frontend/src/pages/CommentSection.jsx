import React from "react";
import {useComments} from "../hooks/useComments";
import CommentCard from "../components/CommentCard";

export default function CommentSection({bugId}) {
    const {comments, loading, error} = useComments(bugId);

    if (loading) return <div>Loading comments...</div>;
    if (error) return <div>Error loading comments: {error.message}</div>;

    return (
        <div className="mt-8">
            <h3 className="text-xl font-bold">Comments</h3>
            <div className="flex flex-col gap-4 mt-4">
                {comments.map(comment => (
                    <CommentCard key={comment.id} comment={comment} />
                ))}
            </div>
        </div>
    );
}