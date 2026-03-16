import React from "react";
import CommentSection from "./pages/CommentSection";

export default function App() {
	return (
		<main className="p-6">
			<h1 className="text-2xl font-bold">BugReporter</h1>
			<CommentSection bugId={1} />
		</main>
	);
}

