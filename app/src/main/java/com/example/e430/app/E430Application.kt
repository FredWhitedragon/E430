package com.example.e430.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.ImageDecoderDecoder

class E430Application : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components {
            add(ImageDecoderDecoder.Factory())
        }
        .crossfade(false)
        .build()
}
