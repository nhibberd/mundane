package com.ambiata.mundane.path

import java.io.File
import org.specs2._
import Arbitraries._

class PathSpec extends Specification with ScalaCheck { def is = s2"""

Path
====

Paths are a recursive data structure, defined in terms of two base
cases, and a recursive case. They represent a hierarchical structure
similar to what one expect for a posix like filesystem. They currently
do _not_ support any windows like functionality that would require a
richer data type to describe what a "Root" is, i.e. drive letter, unc
or similar.

Constructors
------------

  'Path.apply' is symmetrical with 'Path#path':

    ${ prop((p: Path) => Path(p.path) ==== p) }

  'Path.apply' is consistent with 'Path#names':

    ${ prop((ns: List[Component]) => Path(ns.map(_.name).mkString("/")).names ==== ns) }

  'Path.fromList' is consistent with 'Path.apply':

    ${ prop((ns: List[Component]) => Path.fromList(Root, ns) ==== Path("/" + ns.map(_.name).mkString("/"))) }

    ${ prop((ns: List[Component]) => Path.fromList(Relative, ns) ==== Path(ns.map(_.name).mkString("/"))) }

Folds
-----

  Folding over 'Root' base case should always (and only) cause root expression
  to be evaluated:

    ${ prop((x: Int) => Root.fold(x, ???, (_, _) => ???) ==== x) }

  Folding over 'Relative' base case should always (and only) cause relative
  expression to be evaluated:

    ${ prop((x: Int) => Relative.fold(???, x, (_, _) => ???) ==== x) }

  Folding over 'Component' recursive case should always (and only) cause
  component expression to be evaluated:

    ${ prop((x: Int, base: Path, name: Component) =>
         Components(base, name).fold(???, ???, (b, n) => (b, n, x)) ==== ((base, name, x))) }

Combinators
-----------

  'isRoot' should return true if and only if this 'Path' is the top level
  'Root' base case:

    ${ Root.isRoot ==== true }

    ${ Relative.isRoot ==== false }

    ${ prop((base: Path, name: Component) => Components(base, name).isRoot ==== false ) }

  'isRelativeRoot' should return true if and only if this 'Path' is the top
  level 'Relative' base case:

    ${ Root.isRelativeRoot ==== false }

    ${ Relative.isRelativeRoot ==== true }

    ${ prop((base: Path, name: Component) => Components(base, name).isRelativeRoot ==== false ) }

  'dirname' should return 'Root' or 'Relative' for the respective base cases (effectively
  a no-op), or return the base of 'Component' stripping the file name. Posix specifications
  for 'dirname(1) / dirname(3)' should be used to decide any ambiguity.

    ${ Root.dirname ==== Root }

    ${ Relative.dirname ==== Relative }

    ${ prop((base: Path, name: Component) => Components(base, name).dirname ==== base ) }

    ${ (Root | Component("usr") | Component("local")).dirname === (Root | Component("usr")) }

    ${ (Relative | Component("usr") | Component("local")).dirname === (Relative | Component("usr")) }

    ${ (Root | Component("home")).dirname === Root }

    ${ (Relative | Component("home")).dirname === Relative }

  Note also that the result of dirname will always be a prefix of the starting value:

    ${ prop((p: Path) => p.startsWith(p.dirname)) }



  Calling 'parent' where a parent exists, returns 'Some' new path with the top filename
  component stripped off, otherwise it returns 'None'. This behaviour is identical to
  'dirname' except for the base 'Root', 'Relative' cases.

    ${ Root.parent ==== None }

    ${ Relative.parent ==== None }

    ${ prop((base: Path, name: Component) => Components(base, name).parent ==== Some(base) ) }

    ${ prop((base: Path) => base.parent.isDefined ==> { base.parent ==== Some(base.dirname) })  }


  Calling 'basename' where a parent exists, returns 'Some' new path with the top filename
  component stripped off, otherwise it returns 'None'. This behaviour is identical to
  'dirname' except for the base 'Root', 'Relative' cases.

    ${ Root.basename ==== None }

    ${ Relative.basename ==== None }

    ${ prop((base: Path, name: Component) => Components(base, name).basename ==== Some(name) ) }


  'startsWith' on itself should always be true:

    ${ prop((p: Path) => p.startsWith(p) ==== true) }

  'startsWith' on dirname should always be true:

    ${ prop((p: Path) => p.startsWith(p.dirname) ==== true) }

    ${ prop((p: Path) => p.startsWith(p.dirname.dirname) ==== true) }

    ${ prop((p: Path) => p.startsWith(p.dirname.dirname.dirname) ==== true) }

  absolute paths always 'startsWith' Root:

    ${ prop((p: Path) => p.isAbsolute ==> p.startsWith(Root)) }

  absolute paths never 'startsWith' Relative:

    ${ prop((p: Path) => p.isAbsolute ==> !p.startsWith(Relative)) }

  relative paths always 'startsWith' Relative:

    ${ prop((p: Path) => p.isRelative ==> p.startsWith(Relative)) }

  relative paths never 'startsWith' Root:

    ${ prop((p: Path) => p.isRelative ==> !p.startsWith(Root)) }

  'isPrefixOf' is 'startsWith' with the arguments flipped:

    ${ prop((p: Path, q: Path) => p.startsWith(q) ==== q.isPrefixOf(p)) }


  Rendering an absolute path should always start with a '/':

    ${ prop((p: Path) => p.isAbsolute ==> p.path.startsWith("/")) }

  Rendering a relative path should never start with a '/':

    ${ prop((p: Path) => p.isRelative ==> !p.path.startsWith("/")) }

  Rendering a non-root absolute path should always have names.size '/' characters:

    ${ prop((p: Path) => (p.isAbsolute && !p.isRoot) ==> {
                            p.path.filter(_ == '/').size ==== p.names.size } ) }

  Rendering a non-(relative)-root path should always have one less than names.size '/' characters:

    ${ prop((p: Path) => (p.isRelative && !p.isRelativeRoot) ==> {
                          p.path.filter(_ == '/').size ==== (p.names.size - 1) } ) }

  Rendering should contain all component names:

    ${ prop((p: Path) => p.components.forall(p.path.contains _)) }

  Rendering examples:

    ${ Root.path ==== "/" }

    ${ Relative.path ==== "" }

    ${ (Root | Component("usr") | Component("local")).path ==== "/usr/local" }

    ${ (Root | Component("home")).path ==== "/home" }

    ${ (Relative | Component("work")).path ==== "work" }

    ${ (Relative | Component("work") | Component("ambiata")).path ==== "work/ambiata" }

  Rendering with a custom separator matches standard rendering:

    ${ prop((p: Path) => p.path.replace('/', '^') ==== p.pathWith("^")) }

  Path join, a.k.a. '/', verify that the document semantics and invariants
  hold, there is significantly more detail documented on the '/' function,
  but these properties should be self evident in light of the function
  description:

    ${ (Root / Root) ==== Root }

    ${ (Root / Relative) ==== Root }

    ${ (Relative / Root) ==== Root  }

    ${ (Relative / Relative) ==== Relative  }

    ${ prop((p: RelativePath, q: RelativePath) => (p.path.isRelative && q.path.isRelative) ==> (p.path / q.path).isRelative ) }

    ${ prop((p: Path, q: Path) => (p.isAbsolute || q.isAbsolute) ==> (p / q).isAbsolute ) }

    ${ prop((p: Path, q: Path) => q.isAbsolute ==> {  (p / q) ==== q } ) }

    ${ prop((p: Path, q: Path) => q.isRelative ==> { (p / q).components ==== (p.components ++ q.components) } ) }

  Join is associative:

    ${ prop((p: Path, q: Path, r: Path) => ((p / q) / r) ==== (p / (q / r)) ) }

  Join examples:

    ${ ((Root | Component("usr")) / (Relative | Component("local"))) ===
         (Root | Component("usr") | Component("local")) }

    ${ ((Root | Component("usr")) / (Root | Component("home") | Component("mundane"))) ===
         (Root | Component("home") | Component("mundane")) }

    ${ ((Relative | Component("work")) / (Relative | Component("ambiata") | Component("mundane"))) ===
         (Relative | Component("work") | Component("ambiata") | Component("mundane")) }

  'join' is an alias for '/':

    ${ prop((p: Path, q: Path) => (p / q) ==== p.join(q) ) }


  The result of '|' always includes specified Component as final component / basename.

     ${ prop((p: Path, q: Component) => (p | q).names.last ==== q) }

     ${ prop((p: Path, q: Component) => (p | q).basename ==== Some(q) ) }

  Invoking '|' followed by dirname is a no-op on some base path.

     ${ prop((p: Path, q: Component) => (p | q).dirname ==== p ) }

  'extend' is an alias for '|':

    ${ prop((p: Path, q: Component) => (p | q) ==== p.extend(q) ) }


  Any path with the 'Root' base case is absolute:

    ${ Root.isAbsolute }

    ${ prop((p: Path) => (Root / p).isAbsolute ) }


  Any path with the 'Relative' base case is absolute:

    ${ Relative.isRelative }

    ${ prop((ns: List[Component]) => Path.fromList(Relative, ns).isRelative ) }


  'isAbsolute' and 'isRelative' are inverses.

    ${ prop((p: Path) => p.isRelative ^ p.isAbsolute ) }


  'names' contains all filename components (in order):

    ${ prop((ns: List[Component]) => Path.fromList(Relative, ns).names ==== ns ) }

    ${ prop((p: Path, ns: List[Component]) => (p / Path.fromList(Relative, ns)).names ==== (p.names ++ ns) ) }


  'components' is just a stringly typed 'names':

    ${ prop((p: Path) => p.names.map(_.name) ==== p.components ) }


  'rebaseTo' will return 'Some' iff other is a prefix of path.

    ${ prop((p: Path, q: Path) => p.rebaseTo(q).isDefined ==== q.isPrefixOf(p)) }

  'rebaseTo' in the 'Some' case will be the components of 'q' stripped from the components of 'p':

    ${ prop((p: Path, n: Int) => {
         val q = times(p, n, 4) { _.dirname }
         p.rebaseTo(q).map(_.names) ==== Some(p.names.drop(q.names.length)) } ) }

    ${ prop((p: Path, q: Path) => !q.isPrefixOf(p) ==> { p.rebaseTo(q) ==== None } ) }

    ${ prop((p: Path) => p.rebaseTo(p) ==== Some(Relative)) }

    ${ Relative.rebaseTo(Relative) ==== Some(Relative) }

    ${ Root.rebaseTo(Root) ==== Some(Relative) }

    ${ Root.rebaseTo(Relative) ==== None }

    ${ Relative.rebaseTo(Root) ==== None }

"""


  /** apply the transformation f, 'n' times (normalized to 'max' times). */
 def times[A](seed: A, n: Int, max: Int)(f: A => A): A =
    (1 to normalize(n, max)).toList.foldLeft(seed)((x, _) => f(x))

  /** normalize 'n' to between 1 and 'max'. */
  def normalize(n: Int, max: Int): Int =
    (math.abs(n) % 4) + 1
}
