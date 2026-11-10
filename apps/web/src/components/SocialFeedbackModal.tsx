import React, { useState, useEffect } from 'react';
import {
  Star,
  MessageSquare,
  User,
  Heart,
  Users,
  Send,
  Trash2
} from 'lucide-react';
import {
  type Outfit,
  type Rating,
  type Comment,
  type RaterType,
  RaterTypes
} from '@trousseau/shared';
import { ApiClient } from '../api/client.js';

interface SocialFeedbackModalProps {
  outfit: Outfit | null;
  onClose: () => void;
}

export const SocialFeedbackModal: React.FC<SocialFeedbackModalProps> = ({
  outfit,
  onClose
}) => {
  const [ratings, setRatings] = useState<Rating[]>([]);
  const [comments, setComments] = useState<Comment[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // New rating state
  const [raterType, setRaterType] = useState<RaterType>('self');
  const [raterName, setRaterName] = useState('');
  const [stars, setStars] = useState(5);
  const [feedback, setFeedback] = useState('');
  const [isSubmittingRating, setIsSubmittingRating] = useState(false);

  // New comment state
  const [commentText, setCommentText] = useState('');
  const [isSubmittingComment, setIsSubmittingComment] = useState(false);

  useEffect(() => {
    if (!outfit) return;
    loadData();
  }, [outfit?.id]);

  const loadData = async () => {
    if (!outfit) return;
    setIsLoading(true);
    try {
      const [rRes, cRes] = await Promise.all([
        ApiClient.getRatings(outfit.id),
        ApiClient.getComments(outfit.id)
      ]);
      setRatings(rRes.ratings);
      setComments(cRes.comments);
    } finally {
      setIsLoading(false);
    }
  };

  const handleAddRating = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!outfit) return;

    setIsSubmittingRating(true);
    try {
      const name = raterName.trim() || (raterType === 'self' ? 'Myself' : raterType === 'partner' ? 'Partner' : 'Friend');
      const res = await ApiClient.addRating(outfit.id, {
        raterType,
        raterName: name,
        stars,
        feedback: feedback.trim() || undefined
      });
      setRatings([res.rating, ...ratings]);
      setFeedback('');
      setRaterName('');
    } finally {
      setIsSubmittingRating(false);
    }
  };

  const handleAddComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!outfit || !commentText.trim()) return;

    setIsSubmittingComment(true);
    try {
      const res = await ApiClient.addComment(outfit.id, {
        content: commentText.trim()
      });
      setComments([...comments, res.comment]);
      setCommentText('');
    } finally {
      setIsSubmittingComment(false);
    }
  };

  if (!outfit) return null;

  return (
    <div className="fixed inset-0 z-50 bg-stone-900/60 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white dark:bg-stone-900 rounded-3xl max-w-2xl w-full p-6 space-y-6 shadow-2xl max-h-[90vh] flex flex-col border border-stone-200 dark:border-stone-800">
        {/* Header */}
        <div className="flex justify-between items-start border-b border-stone-100 dark:border-stone-800 pb-3">
          <div>
            <span className="text-xs font-bold uppercase tracking-wider text-amber-700 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/60 px-2.5 py-0.5 rounded-full border border-amber-200 dark:border-amber-800">
              {outfit.occasion}
            </span>
            <h3 className="text-xl font-bold font-serif text-stone-900 dark:text-stone-100 mt-1">{outfit.name}</h3>
            <p className="text-xs text-stone-500 dark:text-stone-400">Multi-Perspective Ratings & Styling Discussion</p>
          </div>
          <button onClick={onClose} className="text-stone-400 hover:text-stone-600 dark:hover:text-stone-200 font-bold">
            ✕
          </button>
        </div>

        <div className="flex-1 overflow-y-auto space-y-6 pr-1">
          {/* Section 1: Submit Rating Card */}
          <form onSubmit={handleAddRating} className="bg-stone-50 dark:bg-stone-800/60 rounded-2xl p-4 border border-stone-200 dark:border-stone-700 space-y-3">
            <h4 className="text-xs font-bold text-stone-800 dark:text-stone-200 uppercase tracking-wider">Leave a Star Rating</h4>

            {/* Rater Category Selector */}
            <div className="grid grid-cols-3 gap-2">
              <button
                type="button"
                onClick={() => setRaterType('self')}
                className={`py-2 px-3 rounded-xl text-xs font-semibold flex items-center justify-center space-x-1.5 transition-all ${
                  raterType === 'self'
                    ? 'bg-stone-900 dark:bg-amber-600 text-white shadow-sm'
                    : 'bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-700 text-stone-600 dark:text-stone-300 hover:bg-stone-100 dark:hover:bg-stone-700'
                }`}
              >
                <User className="w-3.5 h-3.5" />
                <span>Personal</span>
              </button>

              <button
                type="button"
                onClick={() => setRaterType('partner')}
                className={`py-2 px-3 rounded-xl text-xs font-semibold flex items-center justify-center space-x-1.5 transition-all ${
                  raterType === 'partner'
                    ? 'bg-rose-700 text-white shadow-sm'
                    : 'bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-700 text-stone-600 dark:text-stone-300 hover:bg-stone-100 dark:hover:bg-stone-700'
                }`}
              >
                <Heart className="w-3.5 h-3.5" />
                <span>Partner</span>
              </button>

              <button
                type="button"
                onClick={() => setRaterType('friend')}
                className={`py-2 px-3 rounded-xl text-xs font-semibold flex items-center justify-center space-x-1.5 transition-all ${
                  raterType === 'friend'
                    ? 'bg-indigo-700 text-white shadow-sm'
                    : 'bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-700 text-stone-600 dark:text-stone-300 hover:bg-stone-100 dark:hover:bg-stone-700'
                }`}
              >
                <Users className="w-3.5 h-3.5" />
                <span>Friends</span>
              </button>
            </div>

            {/* Star selector */}
            <div className="flex items-center space-x-2 pt-1">
              <span className="text-xs text-stone-600 dark:text-stone-400 font-medium">Score:</span>
              <div className="flex space-x-1">
                {[1, 2, 3, 4, 5].map((s) => (
                  <button
                    key={s}
                    type="button"
                    onClick={() => setStars(s)}
                    className="p-1 hover:scale-110 transition-transform"
                  >
                    <Star
                      className={`w-5 h-5 ${
                        s <= stars ? 'text-amber-500 fill-amber-400' : 'text-stone-300 dark:text-stone-600'
                      }`}
                    />
                  </button>
                ))}
              </div>
              <span className="text-xs font-bold text-stone-800 dark:text-stone-200 ml-2">{stars} / 5</span>
            </div>

            {/* Optional name & notes */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              <input
                type="text"
                placeholder={raterType === 'self' ? 'Myself' : "Rater's Name (e.g. Alex)"}
                value={raterName}
                onChange={(e) => setRaterName(e.target.value)}
                className="px-3 py-1.5 bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-700 rounded-xl text-xs text-stone-900 dark:text-stone-100"
              />
              <input
                type="text"
                placeholder="Feedback (e.g. Great color combo)"
                value={feedback}
                onChange={(e) => setFeedback(e.target.value)}
                className="px-3 py-1.5 bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-700 rounded-xl text-xs text-stone-900 dark:text-stone-100"
              />
            </div>

            <button
              type="submit"
              disabled={isSubmittingRating}
              className="w-full py-2 bg-stone-900 hover:bg-stone-800 dark:bg-amber-600 dark:hover:bg-amber-500 text-white rounded-xl text-xs font-semibold transition-colors disabled:opacity-50"
            >
              {isSubmittingRating ? 'Saving Rating...' : 'Save Rating'}
            </button>
          </form>

          {/* Section 2: Ratings List */}
          <div className="space-y-2">
            <h4 className="text-xs font-bold text-stone-800 dark:text-stone-200 uppercase tracking-wider">
              Recorded Ratings ({ratings.length})
            </h4>
            {ratings.length > 0 ? (
              <div className="space-y-2">
                {ratings.map((r) => (
                  <div key={r.id} className="bg-stone-50 dark:bg-stone-800/60 rounded-xl p-3 border border-stone-200 dark:border-stone-700 text-xs flex justify-between items-center">
                    <div>
                      <div className="flex items-center space-x-2">
                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase ${
                          r.raterType === 'partner' ? 'bg-rose-100 text-rose-800 dark:bg-rose-950 dark:text-rose-300' :
                          r.raterType === 'friend' ? 'bg-indigo-100 text-indigo-800 dark:bg-indigo-950 dark:text-indigo-300' :
                          'bg-stone-200 text-stone-800 dark:bg-stone-700 dark:text-stone-200'
                        }`}>
                          {r.raterType}
                        </span>
                        <span className="font-bold text-stone-900 dark:text-stone-100">{r.raterName}</span>
                        <div className="flex">
                          {Array.from({ length: r.stars }).map((_, i) => (
                            <Star key={i} className="w-3 h-3 text-amber-500 fill-amber-400" />
                          ))}
                        </div>
                      </div>
                      {r.feedback && <p className="text-stone-600 dark:text-stone-300 mt-1">{r.feedback}</p>}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-xs text-stone-400 dark:text-stone-500">No ratings yet for this outfit.</p>
            )}
          </div>

          {/* Section 3: Comments & Style Discussion */}
          <div className="space-y-3 pt-2 border-t border-stone-100 dark:border-stone-800">
            <h4 className="text-xs font-bold text-stone-800 dark:text-stone-200 uppercase tracking-wider">
              Discussion & Comments ({comments.length})
            </h4>

            {comments.map((c) => (
              <div key={c.id} className="bg-stone-50 dark:bg-stone-800/60 rounded-xl p-3 border border-stone-200 dark:border-stone-700 text-xs space-y-1">
                <div className="flex justify-between items-center">
                  <span className="font-bold text-stone-900 dark:text-stone-100">{c.userName}</span>
                  <span className="text-[10px] text-stone-400 dark:text-stone-500">
                    {new Date(c.createdAt).toLocaleDateString()}
                  </span>
                </div>
                <p className="text-stone-700 dark:text-stone-300">{c.content}</p>
              </div>
            ))}

            <form onSubmit={handleAddComment} className="flex space-x-2 pt-1">
              <input
                type="text"
                required
                placeholder="Share a thought on this outfit..."
                value={commentText}
                onChange={(e) => setCommentText(e.target.value)}
                className="flex-1 px-3 py-2 bg-stone-50 dark:bg-stone-800 border border-stone-200 dark:border-stone-700 rounded-xl text-xs text-stone-900 dark:text-stone-100 focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-600"
              />
              <button
                type="submit"
                disabled={isSubmittingComment || !commentText.trim()}
                className="px-4 py-2 bg-stone-900 hover:bg-stone-800 dark:bg-amber-600 dark:hover:bg-amber-500 text-white rounded-xl text-xs font-semibold transition-colors disabled:opacity-50 flex items-center space-x-1"
              >
                <Send className="w-3.5 h-3.5" />
                <span>Post</span>
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};
