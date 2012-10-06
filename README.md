# A Friend Tutorial, Using the `interactive-form` Workflow

As a Ruby on Rails developer, I've long been accustomed to having amazing, open-source libraries available for getting a web app up and running quickly. And one of the most important requirements of most web apps is providing some form of authentication and authorization.  In the Ruby on Rails eco-system, this includes great libraries like Devise, Warden, CanCan, and more.

Doing web app development in Clojure, at the present time the tools are not nearly at the same state of maturity.  Earlier this year, while considering the challenge of providing a generalized, modular system for authentication and authorization, Chas Emerick introduced a library called [Friend][1] which aims to provide some of the necessary foundation for providing this kind of system.

To see Friend in action, I'll write a simple login form using [Compojure][2], with the good old email + password credentials setup.

To see Friend's authentication working right away, we can write a very simple app in Compojure, which does nothing other than prevent an un-authenticated user from accessing a page with a simple authorization applied:

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

This doesn't really do much, but shows you basically how Friend wraps up your routes, locking authorized routes down, right out of the box; if you go to the path `/authorized`, you'll see you get redirected immediately to `/login`.  Notice that our routes are not structured any differently than what default Compojure scaffolding provides, we simply intercept the routes with Friend's `authenticate` function before they are passed to Compojure's `site` function.  And in order to trigger the authentication/authorization functionality in Friend, we wrap the response in our route with Friend's `authorize` function, passing in the role which is authorized to access this route as the first argument.

By default, Friend will hand back a redirect to the path `/login` for paths which need to be authenticated, but you can configure this in the second argument to authenticate.  Right now it's just an empty map, as you can see, but this is where you set up most of Friend's functionality outside of configuring your routes.

Right now we don't have any way to login, so let's resolve that by setting up a **workflow.**  Friend uses the concept of the workflow as a way to describe the method by which a user logs in.  This can encompass basic HTTP auth, a simple form, OpenID (the previous three are provided in Friend as default workflows you can use), as well as Oauth1/[Oauth2][3], [Persona][4], or any other system.

This means that if the login method you want to use doesn't exist, you can use Friend to provide higher-level authentication and authorization abstractions, and concern yourself only with implementing the workflow for authentication with that login method.  It also simplifies decoupling your authorization scheme from your authentication, so that the same authorization scheme can work with multiple authentication workflows transparently.

So we have a way to actually login to this app, we'll set up the interactive-form workflow which comes with Friend.  Let's add that to the map we pass to the `authenticate` function:

````clojure
(def app
  (handler/site
   (friend/authenticate app-routes
   			{:credential-fn (partial creds/bcrypt-credential-fn users)
                         :workflows [(workflows/interactive-form)]})))
````

I've cheated a bit by copying some example code from the [Friend README][1].  But it includes a bit more than I've explained up until this point: what is the `credential-fn` key, and what is the `bcrypt-credential-fn` function doing in there?  What is `users`?

Well, the other thing that Friend lets you do is pass in a function defining how you want to parse a user's credentials during the authentication workflow.  This is what `credential-fn` is doing.  As it says in the [Friend README][1]: *Workflows use a credential function to verify the credentials provided to them via requests. Credential functions can be specified either as a :credential-fn option to cemerick.friend/authenticate, or often as an (overriding) :credential-fn option to individual workflow functions.*

So how does bcrypt-credential-fn work?  Well, Mr. Emerick intelligently chose to use [bcrypt as the default option to ensure security][5] for your user's passwords.  And the docs for `bcrypt-credential-fn` state that this function expects *a function of one argument that will look up stored user credentials given a username/id*.  So, all we really need here is a map with the usernames mapped to the bcrypt-hashed passwords.  Luckily, Friend also gives us a bcrypt-hashing function, so we'll use that.

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

You'll see that we have some extra information in here as well: authorization configuration in addition to the authentication credentials.  We'll get to that in a minute, although if you've read the routes above carefully, you may already have a good idea how this is used.

So, we've theoretically done everything we need to do to get a workflow (`interactive-form`) working, and we have our credentials configured and ready to go--we just have to get the HTML scaffolding in place.  This won't win any design usability awards, but for now we'll try this vanilla HTML:

````HTML
<h2>Login</h2>

<form action="/login" method="POST">
Username: <input type="text" name="username" value="" /><br />
Password: <input type="password" name="password" value="" /><br />
<input type="submit" name="submit" value="submit" /><br />
</form>
````

Friend is set up to redirect to `/login` (GET) on a failed authentication, but what is somewhat buried in the README is that the interactive-form workflow has `/login` (POST) is set up to receive credentials and test against those credentials.  So, once you've created the above, you should be able to login.

What about authorization?  We've seen how Friend enables easy configuration of an authentication workflow, but how can we restrict access based on the roles configured in our `users` map?  Well, first let's try creating a new route that is restricted just to administrators:

````clojure
  (GET "/admin" request
       (friend/authorize #{::admin} "This page can only be seen by administrators."))
````

You can see this has the same form as our `/authorized` route, but instead of passing in `{::user}` we are passing in `{::admin}` as our first argument.  You've probably realized by now this matches the `roles` key in the `users` map we defined earlier.  So what happens if we got to `/admin` while still logged in as "dave?"

```` 
Sorry, you do not have access to this resource.  
````

(That's the default message provided as a response by Friend.)  Let's set up a route for logging out so we can test our admin login:

````clojure
(friend/logout (ANY "/logout" request (ring.util.response/redirect "/"))) # also taken from the Friend README
````

And then when we got to `/admin` and login again, this time as admin, we get the message we were hoping:

````
This page can only be seen by administrators.
````

That's great!  Now if we go to the `/authorized` page, we should still be able to get in, right?  Nope...

````
Sorry, you do not have access to this resource.  
````

That doesn't make any sense, admins should have more rights than users, shouldn't they?  So I guess we have to add the `users` role to the `admin` user's roles?  That seems kind of silly...but luckily we don't have to.  As is written in the README, because the `authorized?` check uses `isa?`, *...you can take advantage of Clojure's hierarchies via derive to establish relationships between roles.*  So we can just do this:

````clojure
(derive ::admin ::user)
````

And voila, it works.

There you have it, a very flexible authentication and authorization framework for your app which uses very little configuration and coding to get going.

[1]: https://github.com/cemerick/friend
[2]: https://github.com/weavejester/compojure
[3]: https://github.com/ddellacosta/friend-oauth2
[4]: http://www.mozilla.org/persona/
[5]: http://codahale.com/how-to-safely-store-a-password/

Copyright © 2012 Dave Della Costa
