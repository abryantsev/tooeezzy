package com.tooe.core.util

object BuilderHelper {

  implicit class BuilderWrapper[Builder](val b: Builder) extends AnyVal {
    @deprecated("use one of the others") def extendIf(f: Builder => Builder, condition: Boolean): Builder = if (condition) f(b) else b
    def extend(f: Builder => Builder): Builder = f(b)
    def extend(cond: Boolean)(t: Builder => Builder, f: Builder => Builder = (b) => b): Builder = if (cond) extend(t) else extend(f)
    def extend[V](opt: Option[V])(f: V => Builder => Builder): Builder = opt map (v => extend(f(v))) getOrElse b

    def applyIfDefined[A, T[_]](holder: T[A])(f: (Builder, A) => Builder)(implicit ev: T[A] => Traversable[A]): Builder =
      holder.foldLeft(b)((acc, app) => f(acc, app))
  }
}