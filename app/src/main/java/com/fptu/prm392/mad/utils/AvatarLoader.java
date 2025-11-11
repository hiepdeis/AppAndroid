package com.fptu.prm392.mad.utils;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.fptu.prm392.mad.R;

public class AvatarLoader {

    /**
     * Load avatar image into ImageView using Glide
     * @param context Context
     * @param avatarUrl Avatar URL (can be null or empty)
     * @param imageView Target ImageView
     */
    public static void loadAvatar(Context context, String avatarUrl, ImageView imageView) {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            // Load default avatar
            imageView.setImageResource(R.drawable.profile);
        } else {
            // Load avatar from URL with Glide
            Glide.with(context)
                    .load(avatarUrl)
                    .placeholder(R.drawable.profile)  // Show while loading
                    .error(R.drawable.profile)        // Show on error
                    .circleCrop()                     // Make it circular
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache for performance
                    .into(imageView);
        }
    }

    /**
     * Load avatar without circular crop (for full view)
     * @param context Context
     * @param avatarUrl Avatar URL (can be null or empty)
     * @param imageView Target ImageView
     */
    public static void loadAvatarNoCrop(Context context, String avatarUrl, ImageView imageView) {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            imageView.setImageResource(R.drawable.profile);
        } else {
            Glide.with(context)
                    .load(avatarUrl)
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView);
        }
    }

    /**
     * Clear Glide cache for specific image
     * @param context Context
     * @param imageView Target ImageView
     */
    public static void clearCache(Context context, ImageView imageView) {
        Glide.with(context).clear(imageView);
    }
}

