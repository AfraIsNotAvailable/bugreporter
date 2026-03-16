import React from "react";

export default function CommentCard({comment}) {
    return (
        <div className="p-4 border border-gray-200 rounded-lg shadow-sm">
            <p className="text-gray-800">{comment.text}</p>
            <div className="flex items-center gap-4 mt-3 text-sm text-gray-500">
                <span className="font-semibold text-purple-600">Score: {comment.score}</span>
                <span>By User #{comment.authorId}</span>
            </div>
        </div>
    );
}