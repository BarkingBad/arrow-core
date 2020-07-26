package arrow.continuations.adt

import kotlin.coroutines.suspendCoroutine

typealias Reset<A> = Continuation.Reset<A>
typealias Scope<A> = Continuation.Scope<A>
typealias Shift<A, B> = Continuation.Scope<A>.Shift<B>
typealias Invoke<A, B> = Continuation<A, B>.Invoke
typealias Intercepted<A> = Continuation.Intercepted<A>
typealias KotlinContinuation<A> = kotlin.coroutines.Continuation<A>

sealed class Continuation<A, B> {
  data class Reset<A>(val body: suspend Scope<A>.() -> A) : Continuation<A, Any?>()
  data class Intercepted<A>(val continuation: KotlinContinuation<A>, val prompt: Continuation<*, *>) : Continuation<A, Any?>()
  inner class Invoke(value: A) : Continuation<B, A>()
  abstract class Scope<A> {
    inner class Shift<B>(block: suspend Scope<B>.(Continuation<B, A>) -> A) : Continuation<B, A>()
  }
}

suspend fun <A> reset(body: suspend Scope<A>.() -> A): A =
  suspendCoroutine {
    Intercepted(it, Reset(body)).compile()
  }

suspend fun <A, B> Scope<A>.shift(block: suspend Scope<B>.(Continuation<B, A>) -> A): B =
  suspendCoroutine {
    Intercepted(it, Shift(block)).compile()
  }

suspend operator fun <A, B> Continuation<A, B>.invoke(value: A): B =
  suspendCoroutine {
    Intercepted(it, Invoke(value)).compile()
  }

fun <A, B> Continuation<A, B>.compile(): A =
  when (this) {
    is Reset -> TODO()
    is Shift -> TODO()
    is Invoke -> TODO()
    is Intercepted -> TODO()
  }

class ListScope<A> : Scope<List<A>>() {
  suspend inline operator fun <B> List<B>.invoke(): B =
    shift { cb ->
      this@invoke.flatMap {
        cb(it)
      }
    }
}

inline fun <A> list(block: ListScope<*>.() -> A): List<A> =
  listOf(block(ListScope<A>()))


suspend fun main() {
  val result = list {
    val a = listOf(1, 2, 3)()
    val b = listOf("a", "b", "c")()
    "$a$b "
  }
  println(result)
}

