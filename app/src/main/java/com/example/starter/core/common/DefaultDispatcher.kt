package com.example.starter.core.common

import javax.inject.Qualifier

/** CPU-bound work (tree traversal, sorting, diffing). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
