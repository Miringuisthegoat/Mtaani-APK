package com.benjamin.mtaani.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Represents a community photo post in Firestore.
 *
 * Firestore collection: "community_posts"
 * Each document holds the Cloudinary URL, poster info, and upvote data.
 */
data class CommunityPost(
    @DocumentId
    val id: String = "",

    /** Cloudinary secure URL of the uploaded image */
    val imageUrl: String = "",

    /** Cloudinary public_id — useful for transformations/deletion */
    val cloudinaryPublicId: String = "",

    /** UID of the user who posted */
    val postedBy: String = "",

    /** Display name of the poster */
    val posterName: String = "",

    /** Optional caption */
    val caption: String = "",

    /** List of UIDs who upvoted this post */
    val upvotedBy: List<String> = emptyList(),

    /** Denormalized count for quick sorting */
    val upvoteCount: Int = 0,

    @ServerTimestamp
    val createdAt: Date? = null
)