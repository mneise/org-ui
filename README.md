# org-ui

## Development

Start a REPL (in a terminal: `lein repl`, or from Emacs: open a
clj/cljs file in the project, then do `M-x cider-jack-in`. Make sure
CIDER is up to date).

In the REPL do

```clojure
(run)
(browser-repl)
```

The call to `(run)` does two things, it starts the webserver at port
10555, and also the Figwheel server which takes care of live reloading
ClojureScript code and CSS. Give them some time to start.

When you see the line `Successfully compiled "resources/public/app.js"
in 21.36 seconds.`, you're ready to go. Browse to
`http://localhost:10555` and enjoy.
