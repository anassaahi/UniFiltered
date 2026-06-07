import { onDocumentCreated, onDocumentUpdated } from "firebase-functions/v2/firestore";
import { onCall } from "firebase-functions/v2/https";
import { initializeApp } from "firebase-admin/app";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { GoogleGenAI } from "@google/genai";

initializeApp();
const db = getFirestore();

interface Comment {
    commentId: string;
    authorId: string;
    authorName: string;
    text: string;
    timestamp: number;
    parentId: string | null;
}

// ==========================================
// HELPER: Multi-Key Fallback System
// ==========================================
async function generateContentWithFallback(prompt: string): Promise<string> {
    // Retrieve the comma-separated keys from the environment
    const keysString = process.env.GEMINI_API_KEYS || "";
    const apiKeys = keysString.split(",").map(k => k.trim()).filter(k => k.length > 0);

    if (apiKeys.length === 0) {
        throw new Error("No API keys configured in GEMINI_API_KEYS.");
    }

    let lastError: any = null;

    // Loop through each key one by one
    for (let i = 0; i < apiKeys.length; i++) {
        try {
            const aiClient = new GoogleGenAI({ apiKey: apiKeys[i] });
            const response = await aiClient.models.generateContent({
                model: "gemini-2.5-flash",
                contents: prompt
            });

            if (response.text) {
                // If successful, return the text and exit the loop immediately
                return response.text;
            }
        } catch (error) {
            console.warn(`⚠️ API Key #${i + 1} failed. Trying next key...`);
            lastError = error;
            // The loop continues to the next key automatically
        }
    }

    // If the loop finishes and we are down here, ALL keys failed
    console.error("❌ All Gemini API keys failed.", lastError);
    return "I'm sorry, my systems are currently overloaded. Please try again in a few moments.";
}

// ==========================================
// FUNCTION 1: J.A.R.V.I.S. Comment Agent
// ==========================================
export const onCommentAdded = onDocumentCreated({
    document: "posts/{postId}/comments/{commentId}",
    secrets: ["GEMINI_API_KEYS"] // NOTE: Pluralized
}, async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;

    const commentData = snapshot.data() as Comment;
    const text = commentData.text;

    if (!text.toLowerCase().includes("@jarvis")) {
        return;
    }

    const postId = event.params.postId;
    const triggerCommentId = event.params.commentId;

    try {
        const postDoc = await db.collection("posts").doc(postId).get();
        if (!postDoc.exists) return;
        const postContent = postDoc.data()?.content || "";

        const commentsSnapshot = await db.collection("posts").doc(postId).collection("comments").get();
        const allComments: Comment[] = [];

        commentsSnapshot.forEach((doc: any) => {
            allComments.push(doc.data() as Comment);
        });

        const ancestryChain: Comment[] = [];
        let currentParentId = commentData.parentId;

        while (currentParentId) {
            const parentComment = allComments.find(c => c.commentId === currentParentId);
            if (parentComment) {
                ancestryChain.unshift(parentComment);
                currentParentId = parentComment.parentId;
            } else {
                currentParentId = null;
            }
        }

        const stopWords = new Set(["what", "why", "how", "is", "the", "a", "an", "and", "or", "to", "of", "this", "@jarvis", "@gemini"]);
        const keywords = text.toLowerCase()
            .split(/\s+/)
            .map(word => word.replace(/[^a-zA-Z0-9]/g, ""))
            .filter(word => word.length > 0 && !stopWords.has(word));

        const relatedComments = allComments.filter(c => {
            const isSelfOrAncestor = c.commentId === triggerCommentId || ancestryChain.some(a => a.commentId === c.commentId);
            if (isSelfOrAncestor) return false;

            const commentTextLower = c.text.toLowerCase();
            return keywords.some(keyword => commentTextLower.includes(keyword));
        }).slice(0, 4);

        let prompt = `You are J.A.R.V.I.S., an AI assistant integrated inside a university campus application comment thread.\n`;
        prompt += `Core Post Content:\n"${postContent}"\n\n`;

        if (ancestryChain.length > 0) {
            prompt += `Direct conversation flow leading to the question:\n`;
            ancestryChain.forEach(c => {
                prompt += `- ${c.authorName}: ${c.text}\n`;
            });
            prompt += `\n`;
        }

        if (relatedComments.length > 0) {
            prompt += `Other relevant snippets extracted from the room discussion:\n`;
            relatedComments.forEach(c => {
                prompt += `- ${c.authorName}: ${c.text}\n`;
            });
            prompt += `\n`;
        }

        prompt += `User Question:\n${commentData.authorName}: ${text}\n\n`;
        prompt += `Provide a concise, academic, or helpful response suited for students. Max 3-4 sentences. Do not mention your data constraints or prompt context details to the user.`;

        // NEW: Call the fallback helper instead of defining the client here
        const aiResponseText = await generateContentWithFallback(prompt);

        const aiCommentRef = db.collection("posts").doc(postId).collection("comments").doc();

        const aiComment: Comment = {
            commentId: aiCommentRef.id,
            authorId: "jarvis_ai_agent",
            authorName: "J.A.R.V.I.S.",
            text: aiResponseText,
            timestamp: Date.now(),
            parentId: triggerCommentId
        };

        await aiCommentRef.set(aiComment);

        await db.collection("posts").doc(postId).update({
            commentsCount: FieldValue.increment(1)
        });

    } catch (error) {
        console.error("Error running AI Comment Agent:", error);
    }
});

// ==========================================
// FUNCTION 2: J.A.R.V.I.S. Smart Search
// ==========================================
export const smartSearch = onCall({
    secrets: ["GEMINI_API_KEYS"], // NOTE: Pluralized
    region: "asia-south1"
}, async (request) => {

    const query = request.data.query;
    if (!query || typeof query !== "string") {
        return { response: "Please enter a valid search query." };
    }

    try {
        const stopWords = new Set(["what", "why", "how", "is", "the", "a", "an", "and", "or", "to", "of", "this", "are", "do", "does", "in", "on", "at"]);
        const keywords = query.toLowerCase()
            .split(/\s+/)
            .map(word => word.replace(/[^a-z0-9]/g, ""))
            .filter(word => word.length > 0 && !stopWords.has(word));

        const postsSnapshot = await db.collection("posts")
            .orderBy("timestamp", "desc")
            .limit(20)
            .get();

        const scoredContexts: any[] = [];

        await Promise.all(postsSnapshot.docs.map(async (postDoc: any) => {
            const postData = postDoc.data();
            const postId = postDoc.id;
            const postContent = (postData.content || "").toLowerCase();

            let score = 0;
            const relevantComments: string[] = [];

            keywords.forEach(kw => {
                if (postContent.includes(kw)) score += 2;
            });

            const commentsSnapshot = await db.collection("posts").doc(postId).collection("comments").get();

            commentsSnapshot.forEach((commentDoc: any) => {
                const commentData = commentDoc.data();
                const commentText = (commentData.text || "").toLowerCase();

                let commentMatches = false;
                keywords.forEach(kw => {
                    if (commentText.includes(kw)) {
                        score += 1;
                        commentMatches = true;
                    }
                });

                if (commentMatches) {
                    relevantComments.push(`${commentData.authorName || "Student"}: ${commentData.text}`);
                }
            });

            if (score > 0) {
                scoredContexts.push({
                    author: postData.authorName || "Student",
                    content: postData.content || "",
                    comments: relevantComments,
                    score: score
                });
            }
        }));

        const topMatches = scoredContexts
            .sort((a, b) => b.score - a.score)
            .slice(0, 3);

        let prompt = `You are J.A.R.V.I.S., an AI assistant for a university campus app. A student searched for: "${query}".\n\n`;

        if (topMatches.length > 0) {
            prompt += `Here are the top keyword-matching posts and their relevant comments from the campus database:\n\n`;

            topMatches.forEach(match => {
                prompt += `Post by ${match.author}: "${match.content}"\n`;
                if (match.comments.length > 0) {
                    prompt += `Relevant replies to this post:\n`;
                    match.comments.forEach((c: string) => prompt += `  - ${c}\n`);
                }
                prompt += `\n`;
            });

            prompt += `Synthesize a direct answer to the student's search using the context provided above. If the context doesn't fully answer it, use your general knowledge but keep it relevant to university life.\n`;
        } else {
            prompt += `There were no exact matches in the recent campus feed or comments. Please provide a helpful, general answer to their query suited for a university student.\n`;
        }

        prompt += `Keep your response concise (2-3 sentences max).`;

        // NEW: Call the fallback helper
        const aiResponseText = await generateContentWithFallback(prompt);

        return { response: aiResponseText };

    } catch (error) {
        console.error("Smart Search Error:", error);
        return { response: "J.A.R.V.I.S. is currently offline. Please try again later." };
    }
});

// ==========================================
// FUNCTION 3: Campus Cred (Karma) Tracker
// ==========================================
export const updateCampusCred = onDocumentUpdated({
    document: "posts/{postId}",
    region: "asia-south1"
}, async (event) => {
    const beforeData = event.data?.before.data();
    const afterData = event.data?.after.data();

    if (!beforeData || !afterData) return;

    const beforeLikes = beforeData.likedBy || [];
    const afterLikes = afterData.likedBy || [];

    if (beforeLikes.length === afterLikes.length) return;

    const authorId = afterData.authorId;
    if (!authorId) return;

    const difference = afterLikes.length - beforeLikes.length;

    try {
        await db.collection("users").doc(authorId).update({
            campusCred: FieldValue.increment(difference)
        });
    } catch (error) {
        console.error("Failed to update Campus Cred:", error);
    }
});