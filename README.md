# A Friend Tutorial, Using the `interactive-form` Workflow

One of the most important requirements of most web apps is providing some form of authentication and authorization. And as a Ruby on Rails developer, I've long been accustomed to having amazingly full-featured, easily configurable open-source authentication and authorization libraries available, libraries that integrate well and provide generalized solutions.  This includes libraries like Devise, Warden, CanCan, Omniauth and more.  But until now Clojure hasn't had anything remotely like this--you'd have to role your own.

However, earlier this year, while considering the challenge of providing a generalized, modular system for authentication and authorization, Chas Emerick introduced a library called [Friend][1].  Friend aims to provide some of the necessary foundations for this kind of flexible authentication/authorization system, along the lines of other libraries already present in other language eco-systems.

## How do you use Friend?

Rather than explaining it in detail ([the README does a good job of explaining the details and thought behind it][1]), I'll practice the "show don't tell" method: in the following tutorial, I'll write a simple login form using [Compojure][2], using a standard email + password credentials setup, to show you how Friend can be plugged into a Clojure web app quite simply.

To see Friend's authentication working right away, we can simply wrap some of our Compojure functions in the authentication and authorization filters which Friend provides.  The example below does nothing other than prevent an unauthenticated user from accessing a page with a simple authorization applied to it:

````clojure
(defroutes app-routes
  (GET "/" [] "Hello World")
  (GET "/authorized" request
       (friend/authorize #{::user} "This page can only be seen by authenticated users."))
  (GET "/login" [] "Here is our login page.")
  (route/not-found "Not Found"))

(def app
  (handler/site
   (friend/authenticate app-routes {})))
````

This doesn't really do much, but shows you how to set up an app so Friend will intercept your routes and lock authorized routes down right out of the box: if you go to the path `/authorized`, you'll see you get redirected immediately to `/login`.

Notice that our routes are not structured any differently than what the default Compojure scaffolding provides, we simply intercept the routes with Friend's `authenticate` function before they are passed to Compojure's `handler/site`.  And in order to trigger the authentication/authorization functionality in Friend in our routes, we wrap our response with Friend's `authorize` function, passing in the role which is authorized to access this route as the first argument (roles will be explained in more detail later on).

## Setting up the `interactive-form` Workflow

By default, Friend will hand back a redirect to `/login` when an unauthenticated user hits an authorized path (you can configure this in the second argument to authenticate, what is now an empty map, if you so desire--this is described in the [README][1] in detail).  However, right now we don't have any way to login, so we'll resolve that by setting up a **workflow.**

Friend uses the concept of the workflow as a way to describe the method by which a user logs in.  This can encompass basic HTTP auth, a simple form, OpenID (the previous three workflows are provided in Friend as default workflows you can use), as well as Oauth1/[Oauth2][3], [Persona][4], or any other protocol.

This means that if the login method you want to use doesn't exist, you can use Friend to provide higher-level authentication and authorization abstractions, and concern yourself only with implementing the workflow for authentication with that login method.  It also simplifies decoupling your authorization scheme from your authentication, so that the same authorization scheme can work with multiple authentication workflows transparently.

So we can actually login to this app, we'll set up the interactive-form workflow which comes with Friend.  Let's add that to the map we pass to the `authenticate` function, which was empty in the example above:

````clojure
(def app
  (handler/site
   (friend/authenticate app-routes
   			{:credential-fn (partial creds/bcrypt-credential-fn users)
                         :workflows [(workflows/interactive-form)]})))
````

I've cheated a bit by copying some example code from the [Friend README][1].  But it includes a bit more than I've explained up until this point: what is the `credential-fn` key, and what is the `bcrypt-credential-fn` function doing in there?  What is `users`?

## Credentials

Well, the other thing that Friend lets you do is pass in a function defining how you want to process a user's credentials during the authentication workflow. This is what `credential-fn` is doing.  As it says in the [Friend README][1]: *Workflows use a credential function to verify the credentials provided to them via requests. Credential functions can be specified either as a :credential-fn option to cemerick.friend/authenticate, or often as an (overriding) :credential-fn option to individual workflow functions.*

That is what bcrypt-credential-fn is, but how does it work?  Well, Mr. Emerick intelligently chose to use [bcrypt as the default option to ensure real security][5] for your user's passwords.  And the docs for `bcrypt-credential-fn` state that this function expects *a function of one argument that will look up stored user credentials given a username/id*.  So, all we really need here is a map with the usernames mapped to the bcrypt-hashed passwords.  Luckily, Friend also gives us a bcrypt-hashing function, so we'll use that.

Let's try it in the REPL to see how it works:

````clojure
user=> (require '[cemerick.friend.credentials :as creds])
nil
user=> (def users { "dave" { :username "dave" :password (creds/hash-bcrypt "password") }})
#'user/users
user=> (creds/bcrypt-credential-fn users {:username "bob" :password "wrong"})
nil
user=> (creds/bcrypt-credential-fn users {:username "dave" :password "alsowrong"})
nil
user=> (creds/bcrypt-credential-fn users {:username "dave" :password "password"})
{:username "dave"}
user=> 
````

You can see in the final example that when you the credentials pass, you get a map with the username returned.  Otherwise, nil.

Moving along, we'll cheat again and copy the users map from the [Friend README][1], tweaking the names and passwords slightly:

````clojure
(def users {"admin" {:username "admin"
                    :password (creds/hash-bcrypt "password")
                    :roles #{::admin}}
            "dave" {:username "dave"
                    :password (creds/hash-bcrypt "password")
                    :roles #{::user}}})
````

You'll see that we have some extra information in here as well: role configuration in addition to the authentication credentials.  We'll get to that in a minute, although if you noticed that we passed in the ::user role to the `/authorized` route in our first example, you may have already guess how the roles are used.

## Making it Actually Work

We've done everything behind the scenes that we need to do to get our `interactive-form` workflow in place, and we have our credentials configured and ready to go--we just have to provide some HTML scaffolding.  This won't win any design usability awards, but for now we'll try this vanilla HTML:

````HTML
<h2>Login</h2>

<form action="/login" method="POST">
Username: <input type="text" name="username" value="" /><br />
Password: <input type="password" name="password" value="" /><br />
<input type="submit" name="submit" value="submit" /><br />
</form>
````

...and we'll alter our route to provide this file instead of the simple text we had before in our `/login` handler:

````clojure
  (GET "/login" [] (ring.util.response/file-response "login.html" {:root "resources"}))
````

As you already know, Friend is set up to redirect to `/login` (GET) on a failed authentication.  But (a bit buried) in the README is that the interactive-form workflow has `/login` (POST) set up to receive credentials and test against those credentials.

So, the HTML above is all you should need to login.  If you try it, you should get the response below on the `/authorized` page:

````
This page can only be seen by authenticated users.
````

## Authorization and Roles

What about authorization?  We've seen how Friend enables easy configuration of an authentication workflow, but how can we restrict access based on the roles configured in our `users` map?  Well, first let's try creating a new route that is restricted just to administrators:

````clojure
  (GET "/admin" request
       (friend/authorize #{::admin} "This page can only be seen by administrators."))
````

You can see this has the same form as our `/authorized` route, but instead of passing in `{::user}` we are passing in `{::admin}` as our first argument.  You've probably realized by now this is configured by the `roles` key in the `users` map we defined earlier.  So what happens if we got to `/admin` while still logged in as "dave?"

```` 
Sorry, you do not have access to this resource.  
````

Let's set up a route for logging out so we can test our admin login.  Friend also provides a helper function for this which will clear out your cached authentication state.  In our routes we'll add:

````clojure
(friend/logout (ANY "/logout" request (ring.util.response/redirect "/"))) # also taken from the Friend README
````

And then when we go to `/admin` and login again, this time as admin, we get the message we were hoping to see:

````
This page can only be seen by administrators.
````

Great!  Now if we go to the `/authorized` page, we should still be able to get in, right?

````
Sorry, you do not have access to this resource.  
````

That doesn't make any sense, admins should have more rights than users, shouldn't they?  So I guess we have to add the `users` role to the `admin` user's roles?  That seems kind of silly, as our authorization system should know basic stuff like "admins" have all the rights that "users" do...but luckily we don't have to do this.  As is written in the README, because the `authorized?` check uses `isa?`, *...you can take advantage of Clojure's hierarchies via derive to establish relationships between roles.*  So we can just do this:

````clojure
(derive ::admin ::user)
````

And voila, our "admin" can see everything our "user" can see.

This just scratches the surface, but there you have it: a very flexible authentication and authorization framework for your app, using very little configuration and coding to get going.

(Please note that all the code used in the tutorial is available in this repository.  If you find any issues with the tutorial, or have edits or suggestions on how to make it more clear, please open up an issue or make a pull request with changes.  Thanks!)

[1]: https://github.com/cemerick/friend
[2]: https://github.com/weavejester/compojure
[3]: https://github.com/ddellacosta/friend-oauth2
[4]: http://www.mozilla.org/persona/
[5]: http://codahale.com/how-to-safely-store-a-password/

Copyright © 2012 Dave Della Costa
