# What are `Wrapped[T]` and `Wrapped.Companion` ? 

I assume you've already started your work on Crowmwell Pipeline and found `Wrapped[T]` trait that contains a bunch of methods.
Also, you probably noticed its companion object with a mindblowing `Companion` trait. Take a look at it if not.
It contains a lot of things that at first glance might look overengineered. Don't worry, you are not the first one who got confused otherwise this page wouldn't exist.\
Here we will figure out why they exist, which problem they solve and how to work with them.\
**Note**. All code in this document is independent and self-contained. You can copy any code snippet, and it should compile without issues. This is done to encourage you to play with it and see how it behaves.

## What is Wrapped[T]

Let's start a new project from scratch without any `Wrapper`.
We will create a project that works with a `User`. Here's how it looks like.
```scala
final case class User(
  id: String,
  name: String,
  gender: String,
  password: String,
  email: String
)
``` 
Basically it is a regular user. Now let's write a simple service for such users.
```scala
final case class User(
  id: String,
  name: String,
  gender: String,
  password: String,
  email: String
)

class UserRepository {
  def get(id: String): User =
    User(
      id,
      s"some_name_$id",
      s"non_binary_gender",
      s"super_secret_password_$id",
      "user@email.com"
    )
}

class UserService(repo: UserRepository) {
  def get(id: String): User = repo.get(id)
}
```
Oh! We've faced a production issue! Turns out our service is used by some route, which takes a user and sends it to frontend.
However, our user contains password and now this password is transferred via network and can be exposed at client side. We should not allow such things! (yes, storing a pure password is a big no-no, but that's not the point).
Let's create a special class without a password and will never return a pure `User`.
```scala
final case class UserView(
  id: String,
  age: Int,
  email: String,
  gender: String,
  name: String,
  department: String,
  avatar: Array[Byte]
)

final case class User(
  id: String,
  name: String,
  age: Int,
  gender: String,
  password: String,
  email: String,
  department: String,
  avatar: Array[Byte]
)

class UserRepository {
  def get(id: String): User =
    User(
      id,
      s"some_name_$id",
      21,
      s"non_binary_gender",
      s"super_secret_password_$id",
      "user@email.com",
      "user_department",
      Array.empty
    )
}

class UserService(repo: UserRepository) {
  def get(id: String): UserView = {
    val user = repo.get(id)
    UserView(
      user.id,
      user.age,
      user.gender,
      user.email,
      user.name,
      user.department,
      user.avatar
    )
  }
}
```
Bugfix is deployed to prod. Couple days later manager runs into our room and screams that clients are furious.
Turns out we started to show their emails as their genders, and they think we are intentionally mocking them. Of course, we never wanted to do it, but now our manager must apologize for it.
How did it happen? I think you already know the answer. Take a look at order of fields in `UserView` class and then at how we are creating it in `UserService`. We didn't follow the order of fields and messed everything.\
One might say that it was obvious and could have been be easily avoided. All we needed to do is to use named parameters, which this is true.
Someone might say that this is because the order of fields in `User` is different from order in `UserView`. Which is also true. 
However, it's not that simple. Usage of named arguments is not required in Scala (which is a good thing). That's why we have to watch closely to ensure that named parameters are used in such "dangerous" places.  
Moreover, `User` and `UserView` classes can be in completely different places (which is not bad either) and `User` might have a lot of different views. Because of it, when we update `User` class we have to go through all views and make sure their fields are in correct order. Of course, we have to keep a close eye on it during code review.\
Do you see the common problem here? Both of the proposed solution doesn't actually solve the problem (rather mitigate it), and we still have to manually control everything.
Unfortunately, if we do something manually then eventually we will make a mistake.
But this is a trivial task, right? All we need to do is to make sure we use right variables in right places. Don't we already have a tool for it? Actually, we do. We call this tool a "compiler".
Now let's try to delegate these checks to it.\
Compiler (roughly) can speak only one language - the type language. Which means if we want it to somehow differentiate fields then we need to give a unique type to every field.
Once we do it we don't have any choice other than fill all fields properly.\
Let's get started!
```scala
final case class UserId(value: String)
final case class Name(value: String)
final case class Age(value: Int)
final case class Gender(value: String)
final case class Password(value: String)
final case class Email(value: String)
final case class Department(value: String)
final case class Avatar(value: Array[Byte])

final case class UserView(
  id: UserId,
  age: Age,
  email: Email,
  gender: Gender,
  name: Name,
  department: Department,
  avatar: Avatar
)

final case class User(
  id: UserId,
  name: Name,
  age: Age,
  gender: Gender,
  password: Password,
  email: Email,
  department: Department,
  avatar: Avatar
)

class UserRepository {
  
  // We still can screw up and fill the wrapper with the wrong data
  // e.g. `Gender(s"super_secret_password_$id")` instead of `Gender(s"non_binary_gender")`
  // let's forget about this unsafe place. Usually it's done by DB libraries that won't mess up
  def get(id: UserId): User =
    User(
      id,
      Name(s"some_name_$id"),
      Age(21),
      Gender(s"non_binary_gender"),
      Password(s"super_secret_password_$id"),
      Email("user@email.com"),
      Department("user_department"),
      Avatar(Array.empty)
    )
}

class UserService(repo: UserRepository) {
  def get(id: UserId): UserView = {
    val user = repo.get(id)
    UserView(
      user.id,
      user.age,
      user.email,
      user.gender,
      user.name,
      user.department,
      user.avatar
    )
  }
}
```

Now there is no way to screw up. We just can't map `User` to `UserView` in a way that it compiles and at the same time make a mistake.
If we mess a few fields the code won't compile. Neat!
However, it does come with some cost.
1) Those wrappers are not free in terms of resources. They consume more memory, and their creation takes some CPU power. Albeit this is a pretty small impact, in the scale of a full project it might affect performance dramatically.
2) Try to print this user to a console. You'll notice that output has changed. Each value is "wrapped" into an extra class. This isn't good, because our tool to improve type safety affected how our objects look like.

Well, let's fix it.
 
```scala
final case class UserId(value: String) extends AnyVal {
  override def toString: String = value
}
final case class Name(value: String) extends AnyVal {
  override def toString: String = value
}
final case class Age(value: Int) extends AnyVal {
  override def toString: String = value.toString
}
final case class Gender(value: String) extends AnyVal {
  override def toString: String = value
}
final case class Password(value: String) extends AnyVal {
  override def toString: String = value
}
final case class Email(value: String) extends AnyVal {
  override def toString: String = value
}
final case class Department(value: String) extends AnyVal {
  override def toString: String = value
}
final case class Avatar(value: Array[Byte]) extends AnyVal {
  override def toString: String = value.toString
}

final case class UserView(
  id: UserId,
  age: Age,
  email: Email,
  gender: Gender,
  name: Name,
  department: Department,
  avatar: Avatar
)

final case class User(
  id: UserId,
  name: Name,
  age: Age,
  gender: Gender,
  password: Password,
  email: Email,
  department: Department,
  avatar: Avatar
)

class UserRepository {
  def get(id: UserId): User =
    User(
      id,
      Name(s"some_name_$id"),
      Age(21),
      Gender(s"non_binary_gender"),
      Password(s"super_secret_password_$id"),
      Email("user@email.com"),
      Department("user_department"),
      Avatar(Array.empty)
    )
}

class UserService(repo: UserRepository) {
  def get(id: UserId): UserView = {
    val user = repo.get(id)
    UserView(
      user.id,
      user.age,
      user.email,
      user.gender,
      user.name,
      user.department,
      user.avatar
    )
  }
}
```

Much better! Small addition `extends AnyVal` will tell the compiler that it doesn't need to wrap our classes and waste resource unless it has to ( read this article for more information https://docs.scala-lang.org/overviews/core/value-classes.html )\
However, it's still not perfect. We must redefine `toString` everywhere and yet again we have to take care of it manually.
Wouldn't it be nice to have some common trait that will redefine required methods automatically?\
Actually we can easily add this trait and call it `Wrapped` :) Let's see how it looks.
```scala
// `extends Any` doesn't have hidden meaning.
// It is here only to bypass Scala limitation on value classes, try to remove it and see what happens
trait Wrapped[T] extends Any {
  def value: T
  override def toString: String = value.toString
}

final case class UserId(value: String) extends AnyVal with Wrapped[String]
final case class Name(value: String) extends AnyVal with Wrapped[String]
final case class Age(value: Int) extends AnyVal with Wrapped[Int]
final case class Gender(value: String) extends AnyVal with Wrapped[String]
final case class Password(value: String) extends AnyVal with Wrapped[String]
final case class Email(value: String) extends AnyVal with Wrapped[String]
final case class Department(value: String) extends AnyVal with Wrapped[String]
final case class Avatar(value: Array[Byte]) extends AnyVal with Wrapped[Array[Byte]]

final case class UserView(
  id: UserId,
  age: Age,
  email: Email,
  gender: Gender,
  name: Name,
  department: Department,
  avatar: Avatar
)

final case class User(
  id: UserId,
  name: Name,
  age: Age,
  gender: Gender,
  password: Password,
  email: Email,
  department: Department,
  avatar: Avatar
)

class UserRepository {
  def get(id: UserId): User =
    User(
      id,
      Name(s"some_name_$id"),
      Age(21),
      Gender(s"non_binary_gender"),
      Password(s"super_secret_password_$id"),
      Email("user@email.com"),
      Department("user_department"),
      Avatar(Array.empty)
    )
}

class UserService(repo: UserRepository) {
  def get(id: UserId): UserView = {
    val user = repo.get(id)
    UserView(
      user.id,
      user.age,
      user.email,
      user.gender,
      user.name,
      user.department,
      user.avatar
    )
  }
}
```
I hope now you see we have added this trait to our project.
However, you probably noticed that real `Wrapped` contains not only `toString`, but also a bunch of other methods.\
In general, we can't be sure this trait will be mixed into something that has a correct implementation of `equals` / `hashcode`. 
That's why we override them. They are necessary to make sure our wrapper will always have correct implementation of `equals` / `hashcode` regardless of original class.
We'll see specific cases when it can be useful a little later.


Now it's time to dive into `Wrapped.Companion`.

## What is Wrapped.Companion
In short, `Wrapped.Companion` serves two purposes.
1) It reminds Scala that our wrappers can be treated as underlying types if needed.
2) It gives us more benefits from having a unique type for different entities rather than having `String`s everywhere.

Just like before, we will work with a `User` and a small service. However, this time we will reduce the amount of fields since it doesn't matter anymore.
We've already established why having different types for different fields is useful, so let's assume we need wrappers here as well and focus on other things.\
Let's start from first `Wrapped.Companion` purpose.

We already saw how addition of wrapper broke existing behaviour using `toString` method as an example. However, there are a lot of other places.
Take a look at the following.
```scala
final case class User(
  id: String,
  age: Int,
  email: String
)

class UserRepository {
  def get(id: String): User =
    User(
      id,
      21,
      "user@email.com"
    )
}

class UserService(repo: UserRepository) {
  def get(id: String): User = repo.get(id)
  
  def sortByEmail(users: Seq[User]): Seq[User] = users.sortBy(_.email)
  def sortByAge(users: Seq[User]): Seq[User] = users.sortBy(_.age)
}
```
Now let's introduce type wrappers
```scala
trait Wrapped[T] extends Any {
  def value: T
  override def toString: String = value.toString
}

final case class UserId(value: String) extends AnyVal with Wrapped[String]
final case class Age(value: Int) extends AnyVal with Wrapped[Int]
final case class Email(value: String) extends AnyVal with Wrapped[String]

final case class User(
  id: UserId,
  age: Age,
  email: Email
)

class UserRepository {
  def get(id: UserId): User =
    User(
      id,
      Age(21),
      Email("user@email.com")
    )
}

class UserService(repo: UserRepository) {
  def get(id: UserId): User = repo.get(id)
  
  def sortByEmail(users: Seq[User]): Seq[User] = {
    // nope
    // users.sortBy(_.email)
    ???
  }
  def sortByAge(users: Seq[User]): Seq[User] = {
   // nope
   // users.sortBy(_.age)
   ???
 }
}
```
Suddenly the code doesn't compile. That's because Scala standard library contains `Ordering` instances for `String` and `Int` and therefore knows how to order them.
However, it doesn't know anything about our wrappers and can't sort them.\
There can be a lot of places that will be broken by such change.
For example, almost all (if not all) Scala JSON libraries know how to `serialize` / `deserialize` `String` or `Int`, but they know nothing about our custom wrappers. Same with database libraries and so on.
Actually, this is a good thing, that's what we wanted! `Email` and `Age` are not just `String` or `Int`, so there is no surprise Scala refuses to see them that way. Yes sometimes we need to treat wrapped type like there is no wrapper at all. So now we have to tell Scala in which cases we don't care about the difference between `String` and `Wrapped[String]`  

Let's do it naively
```scala
trait Wrapped[T] extends Any {
  def value: T
  override def toString: String = value.toString
}

final case class UserId(value: String) extends AnyVal with Wrapped[String]
object UserId {
  implicit val userIdOrdering: Ordering[UserId] = Ordering.by(_.value)
}

final case class Age(value: Int) extends AnyVal with Wrapped[Int]
object Age {
  implicit val ageOrdering: Ordering[Age] = Ordering.by(_.value)
}

final case class Email(value: String) extends AnyVal with Wrapped[String]
object Email {
  implicit val emailOrdering: Ordering[Email] = Ordering.by(_.value)
}

final case class User(
  id: UserId,
  age: Age,
  email: Email
)

class UserRepository {
  def get(id: UserId): User =
    User(
      id,
      Age(21),
      Email("user@email.com")
    )
}

class UserService(repo: UserRepository) {
  def get(id: UserId): User = repo.get(id)
  
  def sortByEmail(users: Seq[User]): Seq[User] = users.sortBy(_.email)
  def sortByAge(users: Seq[User]): Seq[User] = users.sortBy(_.age)
}
```
Now it works, but looks like we have some duplication here which can be moved to a common trait.
```scala
trait Wrapped[T] extends Any {
  def value: T
  override def toString: String = value.toString
}
trait WrappedCompanionObject[Type, Wrapper <: Wrapped[Type]] {
  implicit def wrappedOrdering(implicit originalOrdering: Ordering[Type]): Ordering[Wrapper] = Ordering.by(_.value)
}


final case class UserId(value: String) extends AnyVal with Wrapped[String]
object UserId extends WrappedCompanionObject[String, UserId]

final case class Age(value: Int) extends AnyVal with Wrapped[Int]
object Age extends WrappedCompanionObject[Int, Age]

final case class Email(value: String) extends AnyVal with Wrapped[String]
object Email extends WrappedCompanionObject[String, Email]

final case class User(
  id: UserId,
  age: Age,
  email: Email
)

class UserRepository {
  def get(id: UserId): User =
    User(
      id,
      Age(21),
      Email("user@email.com")
    )
}

class UserService(repo: UserRepository) {
  def get(id: UserId): User = repo.get(id)
  
  def sortByEmail(users: Seq[User]): Seq[User] = users.sortBy(_.email)
  def sortByAge(users: Seq[User]): Seq[User] = users.sortBy(_.age)
}
```
Nice! Now we can fill companion object with whatever we want (JSON serializers, DB integration etc) and every `Wrapped` child will have it.\
**Note**. Alternatively, in this particular case we could have made `Wrapped[T] extends Ordered[T]` instead of defining `Ordering` in companion object. However, it wouldn't work well, try to do it and see what happens.

There are a few differences with real `Wrapped.Companion`
1) `Wrapped.Companion` uses abstract types instead of type parameters.
   Most of the time these approaches are interchangeable. Difference between them is far beyond the scope of this document. So let's just replace type parameters with abstract types for more similarity with the original trait. In this case it doesn't matter and an arbitrary decision.
2) We called our trait `WrappedCompanionObject`, while the real trait is actually called `Companion` and is placed in `Wrapped` companion object.
   This is because in Scala we often put related things to companion objects. 
   Both approaches would work more or less the same, so let's just put our trait to `Wrapped` companion object and rename it to match real `Wrapped.Companion`.

```scala
trait Wrapped[T] extends Any {
  def value: T
  override def toString: String = value.toString
}

object Wrapped {
  trait Companion {
    type Type
    type Wrapper <: Wrapped[Type]
    implicit def wrappedOrdering(implicit originalOrdering: Ordering[Type]): Ordering[Wrapper] = Ordering.by(_.value)
  }
}


final case class UserId(value: String) extends AnyVal with Wrapped[String]
object UserId extends Wrapped.Companion {
  type Type = String
  type Wrapper = UserId
}

final case class Age(value: Int) extends AnyVal with Wrapped[Int]
object Age extends Wrapped.Companion {
  type Type = Int
  type Wrapper = Age
}

final case class Email(value: String) extends AnyVal with Wrapped[String]
object Email extends Wrapped.Companion {
  type Type = String
  type Wrapper = Email
}

final case class User(
  id: UserId,
  age: Age,
  email: Email
)

class UserRepository {
  def get(id: UserId): User =
    User(
      id,
      Age(21),
      Email("user@email.com")
    )
}

class UserService(repo: UserRepository) {
  def get(id: UserId): User = repo.get(id)
  
  def sortByEmail(users: Seq[User]): Seq[User] = users.sortBy(_.email)
  def sortByAge(users: Seq[User]): Seq[User] = users.sortBy(_.age)
}
```
As you can see, our `Wrapped.Compaion` already looks similar to what we have in real code.
One more time we reminded Scala that our custom classes are just thin wrappers on real types.\
However, at this point it starts to look awkward. Introduction of custom wrappers allowed us to gain a lot of type safety.
But at the same time we lost a lot of information and have to teach Scala how to work with them when it comes to trivial things like serialization or sorting.
Sounds like we gain one feature and loose tons of features, which is unfair. Maybe we don't need it after all?\
Yes, we do need it. We just need to make a fair comparison. Let's see what else wrappers can give us.

_Note._ The next section is going to be a bit larger than previous two. However, this is the last thing that we'll need to understand real `Wrapped` implementation.
So don't worry, it will be over soon. 

Again, let's start with a simple example
```scala
final case class User(
  id: String,
  age: Int,
  email: String
)

class UserRepository {
  def get(id: String): User =
    User(
      id,
      21,
      "user@email.com"
    )
}

class UserService(repo: UserRepository) {
  def get(id: String): User = repo.get(id)
}
```
Time passes, and our project grows. Now it can save users, send emails and provide useful information.
```scala
final case class User(
  id: String,
  age: Int,
  email: String
)

object Utils {
  private val dummyEmailRegexp: String = "(\\w|\\d)+@(\\w|\\d)+\\.(\\w|\\d)+"

  def isValidAge(age: Int): Boolean = age >= 0 && age < 200
  def isValidEmail(email: String): Boolean = email.matches(dummyEmailRegexp)
}

class EmailService {
  def sendEmail(email: String, content: String): Unit = {
    if (!Utils.isValidEmail(email)) throw new RuntimeException("email is invalid")
    println(s"Sending [$content] to $email")
  }
}

class LawsInfoService {
  def canHaveRussianPassport(age: Int): Boolean = {
    if (!Utils.isValidAge(age)) throw new RuntimeException("age is invalid")
    age >= 14
  }
}

class UserRepository {
  def get(id: String): User =
    User(
      id,
      21,
      "user@email.com"
    )
    
  def save(user: User): Unit = ()
}

class UserService(repo: UserRepository, laws: LawsInfoService, emailService: EmailService) {
  def get(id: String): User = repo.get(id)
  def save(user: User): Unit = {
    if (!Utils.isValidAge(user.age)) throw new RuntimeException("age is invalid")
    if (!Utils.isValidEmail(user.email)) throw new RuntimeException("email is invalid")
    repo.save(user)
    val message = if (laws.canHaveRussianPassport(user.age)) "hello big buddy, you are saved" else "hello, you are saved"
    emailService.sendEmail(user.email, message)
  }
}
```
The problem here is that we have to make a lot of duplicate checks. `UserService.saveUser` checks that `email` and `age` are valid, otherwise it can't save it to DB.
Then in calls `LawsInfoService.canHaveRussianPassport` and `EmailService.sendEmail`, and they check that `email` and `age` are valid as well.
It's kinda stupid because at that point we already checked that. However, we can't remove these checks because `sendEmail` or `canHaveRussianPassport` can be called anywhere which means we can't assume that input data is always valid.\
Hence, we need some way to validate input data and then "remember" that this data is valid. That's where our `Wrapped` type comes into play. We can make such checks during `Wrapped` creation.
In this case we need to check input data only once and then guarantee that either we don't have a `Wrapped` object, or it contains a valid data.\
This concept is called `smart constructors`. In short, smart constructors are useful when you cannot guarantee that you can create a valid object based on provided data.
For example, we can create an `Email` from `String`, but we can't do it for every `String`, because not every `String` is a valid `Email`.
```scala
final case class Email(value: String)
val email: Email = Email("not an email") // this should not be allowed
```
We have to somehow validate input data. One way is to throw an exception during the construction.
```scala
final case class Email(value: String) {
  private val dummyEmailRegexp: String = "(\\w|\\d)+@(\\w|\\d)+\\.(\\w|\\d)+"
  require(value.matches(dummyEmailRegexp), s"email must match regexp $dummyEmailRegexp")
}
val email: Email = Email("not an email") // compiles, but throws exception at runtime, i.e. object is not created
```
However, we don't throw exceptions in Scala. Instead, we should hide constructor and use factory method that will be the only way to create an instance of our class.
```scala
final class Email private (val value: String)
object Email {
  private val dummyEmailRegexp: String = "(\\w|\\d)+@(\\w|\\d)+\\.(\\w|\\d)+"
  def fromString(value: String): Either[String, Email] =
    if (value.matches(dummyEmailRegexp)) Right(new Email(value)) else Left(s"email must match regexp $dummyEmailRegexp")
    
}
//val email: Email = new Email("not an email") // won't compile anymore
//val email: Email = Email.fromString("not an email") // won't compile anymore
val email: Either[String, Email] = Email.fromString("not an email")
```
Notice that we don't use `case class` anymore. This is because `case class`es by their nature are open data structures that can be easily created. We don't want it anymore.
Now we want to explicitly say that not every string is a valid email and compiler will make sure we didn't forget about it.\
Let's see how it works in our project.

```scala
trait Wrapped[T] extends Any {
  def value: T
  override def toString: String = value.toString
}

final class UserId private (val value: String) extends AnyVal with Wrapped[String]
object UserId {
  // can't actually fail, but let's keep it that way for consistency
  def from(value: String): Either[Nothing, UserId] = Right(new UserId(value))
}

final class Age private (val value: Int) extends AnyVal with Wrapped[Int]
object Age {
  def from(value: Int): Either[String, Age] =
   if (value >= 0 && value < 200) Right(new Age(value)) else Left("user age should be from 0 to 200 years")
}

final class Email private (val value: String) extends AnyVal with Wrapped[String]
object Email {
  private val dummyEmailRegexp: String = "(\\w|\\d)+@(\\w|\\d)+\\.(\\w|\\d)+"
  def from(value: String): Either[String, Email] =
   if (value.matches(dummyEmailRegexp)) Right(new Email(value)) else Left(s"email must match regexp $dummyEmailRegexp")
}

final case class User(
  id: UserId,
  age: Age,
  email: Email
)

class EmailService {
  def sendEmail(email: Email, content: String): Unit =
    println(s"Sending [$content] to $email")
}

class LawsInfoService {
  def canHaveRussianPassport(age: Age): Boolean =
    age.value >= 14
}

class UserRepository {
  def get(id: UserId): User = ???
    // oops, it doesn't work anymore. We will deal with it later
    //User(
    //  id,
    //  Age(21),
    //  Email("user@email.com")
    //)

  def save(user: User): Unit = ()
}

class UserService(repo: UserRepository, laws: LawsInfoService, emailService: EmailService) {
  def get(id: UserId): User = repo.get(id)
  
  def save(user: User): Unit = {
    repo.save(user)
    val message = if (laws.canHaveRussianPassport(user.age)) "hello big buddy, you are saved" else "hello, you are saved"
    emailService.sendEmail(user.email, message)
  }
}
```

Not yet perfect though. There are two problems now:
1. Since we migrated from case classes to regular classes, we no longer have proper `equals` / `hashcode` implementation. 
2. There's a lot of duplication in companion objects 

Remember how I said earlier that we'll see how `equals` / `hashcode` from `Wrapped` can be useful? Well, problem #1 is the reason they exist.
We can fix problem #1 by just overriding these methods in `Wrapped` trait.\
_Note_. It's possible to make case classes not so open and with removed `copy` / `apply` methods.
For example
```scala
sealed abstract case class Email private (value: String)

object Email {
  def fromString(emailStr: String): Option[Email] = {
    Some(new Email(emailStr) {} )
  }
}

```
In this case hashcode and equals still generated by Scala.\
We will use regular classes to better match real project, but maybe you'll find this trick useful somewhere else.


As for the problem #2, we will have to play with `Wrapped.Companion` again. 

```scala
trait Wrapped[T] extends Any {
  def value: T
  override def toString: String = value.toString

  // check that this is the same Wrapper, e.g. that we are comparing `UserId` vs `UserId` and not `UserId` vs `Email`
  def canEqual(that: Any): Boolean = this.getClass.isInstance(that)
  // cast and compare underlying values
  override def equals(that: Any): Boolean = canEqual(that) && this.value.equals(that.asInstanceOf[Wrapped[T]].value)
  // hashcode takes into account wrapper class information and its content. This is done to obey equals / hashcode laws
  override def hashCode: Int = this.getClass.hashCode + value.hashCode()
}

object Wrapped {
  trait Companion {
    type Type
    type Wrapper <: Wrapped[Type]
    
    // we don't have to define such alias, but let's do it in order to show what this Either means
    type ValidationResult[A] = Either[String, A]
    // check that provided data is valid and can be used to create a wrapper
    protected def validate(value: Type): ValidationResult[Type]
    // unsafely creates a wrapper. Without this method we just don't know how to do it for an arbitrary type
    protected def create(value: Type): Wrapper

    // validates data and then unsafely creates wrapper
    final def from(value: Type): ValidationResult[Wrapper] = validate(value).map(create)
  }
}

final class UserId private (val value: String) extends AnyVal with Wrapped[String]
object UserId extends Wrapped.Companion {
  type Type = String
  type Wrapper = UserId
  
  protected def validate(value: String): Either[String, String] = Right(value)
  protected def create(value: String): UserId = new UserId(value)
}

final class Age private (val value: Int) extends AnyVal with Wrapped[Int]
object Age extends Wrapped.Companion {
  type Type = Int
  type Wrapper = Age

  protected def validate(value: Int): Either[String, Int] =
    if (value >= 0 && value < 200) Right(value) else Left("user age should be from 0 to 200 years")

  protected def create(value: Int): Age = new Age(value)
}

final class Email private (val value: String) extends AnyVal with Wrapped[String]
object Email extends Wrapped.Companion {
  type Type = String
  type Wrapper = Email
  
  private val dummyEmailRegexp: String = "(\\w|\\d)+@(\\w|\\d)+\\.(\\w|\\d)+"
  
  protected def validate(value: String): Either[String, String] =
    if (value.matches(dummyEmailRegexp)) Right(value) else Left(s"email must match regexp $dummyEmailRegexp")

  protected def create(value: String): Email = new Email(value)
}

final case class User(
  id: UserId,
  age: Age,
  email: Email
)

class EmailService {
  def sendEmail(email: Email, content: String): Unit =
    println(s"Sending [$content] to $email")
}

class LawsInfoService {
  def canHaveRussianPassport(age: Age): Boolean =
    age.value >= 14
}

class UserRepository {
  def get(id: UserId): User = ???
    // still doesn't work. We will fix it soon
    //User(
    //  id,
    //  Age(21),
    //  Email("user@email.com")
    //)

  def save(user: User): Unit = ()
}

class UserService(repo: UserRepository, laws: LawsInfoService, emailService: EmailService) {
  def get(id: UserId): User = repo.get(id)
  
  def save(user: User): Unit = {
    repo.save(user)
    val message = if (laws.canHaveRussianPassport(user.age)) "hello big buddy, you are saved" else "hello, you are saved"
    emailService.sendEmail(user.email, message)
  }
}
```

We almost there! `Wrapped` trait now looks exactly the same as in real project.
There are only two new issues to solve (this is the last two).
1. Returned error is not flexible enough. What if someone wants to return (not throw!) an exception? What if someone wants to return several errors instead of just one?
2. We can't just create a new object in repository anymore. Which is actually a problem, because our repository works with a trusted source of data.
   We should have a way to tell Scala that sometimes it is ok to let us unsafely create a wrapper and _we_ know that it's going to work fine.

Of course, we can just "fix" problem #1 by saying that most of the time we can represent errors as String so people should just call `.toString` on their errors.
However, we can do better than this and allow people to configure error type using the abstract type.\


Problem #2, however, a bit more interesting. We have made a hard restriction on how we can create a `Wrapper`s, but now we have to lax it.
Actually we can just leave it as is and force people to do something like this `Email.from("some@email.com").getOrElse(throw new RuntimeException("invalid email)`.
But again, it will lead to code duplication, and it's better to have one common place for such things with standard error message to make our life easier.\
Therefore, we need to give people a way to create wrapped instances directly.
Let's do it by introducing new method for unsafe creation. However, we will do this method so ugly, that it would be impossible to use it without being noticed.
Yes, it would be unsafe and an obvious flaw, but hopefully it would be a controlled place that everyone is aware of.\
_Note_. Word "unsafe" here doesn't mean we allow creation from invalid data. It's still strictly forbidden. "Unsafe" means that our factory method will return `Wrapped` directly instead of `Either[Error, Wrapped]` so that people don't have to get rid of this `Either` manually.
I.e. we are replacing compile-time checks with runtime checks.
```scala
trait Wrapped[T] extends Any {
  def value: T
  override def toString: String = value.toString

  def canEqual(that: Any): Boolean = this.getClass.isInstance(that)
  override def equals(that: Any): Boolean = canEqual(that) && this.value.equals(that.asInstanceOf[Wrapped[T]].value)
  override def hashCode: Int = this.getClass.hashCode + value.hashCode()
}

object Wrapped {
  trait Companion {
    type Type
    type Wrapper <: Wrapped[Type]
    type Error
    type ValidationResult[A] = Either[Error, A]
 
    protected def validate(value: Type): ValidationResult[Type]
    protected def create(value: Type): Wrapper

    final def from(value: Type): ValidationResult[Wrapper] = validate(value).map(create)
  
    final def uglyUnsafeCreation(value: Type): Wrapper = from(value) match {
      case Right(wrapper) => wrapper
      case Left(error) => throw new RuntimeException(s"Oh no! You promised me that data is valid but I got an error! $error")
    }
  }
}

final class UserId private (val value: String) extends AnyVal with Wrapped[String]
object UserId extends Wrapped.Companion {
  type Type = String
  type Wrapper = UserId
  type Error = Nothing
  
  protected def validate(value: String): Either[Nothing, String] = Right(value)
  protected def create(value: String): UserId = new UserId(value)
}

final class Age private (val value: Int) extends AnyVal with Wrapped[Int]
object Age extends Wrapped.Companion {
  type Type = Int
  type Wrapper = Age
  type Error = String

  protected def validate(value: Int): Either[String, Int] =
    if (value >= 0 && value < 200) Right(value) else Left("user age should be from 0 to 200 years")

  protected def create(value: Int): Age = new Age(value)
}

final class Email private (val value: String) extends AnyVal with Wrapped[String]
object Email extends Wrapped.Companion {
  type Type = String
  type Wrapper = Email
  type Error = String
  
  private val dummyEmailRegexp: String = "(\\w|\\d)+@(\\w|\\d)+\\.(\\w|\\d)+"
  
  protected def validate(value: String): Either[String, String] =
    if (value.matches(dummyEmailRegexp)) Right(value) else Left(s"email must match regexp $dummyEmailRegexp")

  protected def create(value: String): Email = new Email(value)
}

final case class User(
  id: UserId,
  age: Age,
  email: Email
)

class EmailService {
  def sendEmail(email: Email, content: String): Unit =
    println(s"Sending [$content] to $email")
}

class LawsInfoService {
  def canHaveRussianPassport(age: Age): Boolean =
    age.value >= 14
}

class UserRepository {
  def get(id: UserId): User =
    User(
      id,
      Age.uglyUnsafeCreation(21),
      Email.uglyUnsafeCreation("user@email.com")
    )


  def save(user: User): Unit = ()
}

class UserService(repo: UserRepository, laws: LawsInfoService, emailService: EmailService) {
  def get(id: UserId): User = repo.get(id)
  
  def save(user: User): Unit = {
    repo.save(user)
    val message = if (laws.canHaveRussianPassport(user.age)) "hello big buddy, you are saved" else "hello, you are saved"
    emailService.sendEmail(user.email, message)
  }
}
```

Congratulations, that's it! We have finally done effectively the same thing as we have on the real project.\
There are only two differences with the real project which we won't apply because... we just don't have to. 
1. In the real project, we have ugly `apply` method that takes `Enable.Unsafe` parameter, while we have `uglyUnsafeCreation`. Who cares :)
2. The real project uses classes from `cats` library instead of standard Scala classes (it could have been regular Scala classes, usage of `cats` was an arbitrary decision).
  `Validated` in our case is the same thing as `Either`. `NonEmptyChain` is used to enforce people to give at least one validation error. For simplicity, you can think of `NonEmptyChain` as a `List` with at least one element.


## Conclusion
Now, go to the real project and take a look at `Wrapped` and `Wrapped.Companion`. You should be able to understand the purpose of every line in these traits.\
Still not convinced that it is not an overengineered mess? That's ok! Try to implement the same functionality by yourself.
Odds are you either end up with something very similar to current implementation and understand why do we need every line (which is great) or create a better solution (even better!).
If latter is the case, feel free to open a pull request and fix current implementation.



## Useful information
We are not the first people who faced such problems. A lot of people noticed it way earlier and created libraries that automate such things.\
For example, here's the library that effectively does the same thing as `Wrapped[T]` trait.\
https://github.com/estatico/scala-newtype \
And here's the library that allows you to have compile-time proofs on data validity (smart constructors).\
https://github.com/fthomas/refined \
If you can combine these two libraries, it will give you roughly the same thing as we have on Cromwell Pipeline.\
Why didn't we _just_ use them? That's because this project is a sandbox for people who are just starting their journey in Scala.
We intentionally decided not to use a lot of third-party libraries and focus on vanilla language features. Besides, these libraries heavily use macro. Macro world is full of magic even for an experienced developer, let alone beginners.
