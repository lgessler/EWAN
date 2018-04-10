# ewan

This will be an experimental port of a minimal feature-subset of [ELAN](https://tla.mpi.nl/tools/tla-tools/elan/) into the browser. I am using a variety of tools I'm excited about, including [ClojureScript](https://clojurescript.org/),  [re-frame](https://github.com/Day8/re-frame), [PouchDB](https://pouchdb.com/), and [Material-UI](http://www.material-ui.com/).

# Roadmap

☑️ Translate [EAF XSD](http://www.mpi.nl/tools/elan/EAFv3.0.xsd) into a Clojure [spec](https://clojure.org/guides/spec)

☐ Support for most basic ELAN workflows

☐ Export and import of EAF

☐ Full ELAN functionality coverage

☐ Memote syncing of projects (this also entails accounts and auth)

☐ Live collaborative editing *à la* Google Docs

☐ Plugin system letting users write custom scripts

# Building

## Compile css:

```sh
lein less once 
lein less auto
```

## Run application:

```
lein clean
lein figwheel dev
```

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449). Code will automatically reload when there have been changes made, but application state will remain the same, which can sometimes create unrealistic results. Refresh the page if you have made very major code changes.

## Run tests:

```
lein clean
lein doo phantom test once
```

The above command assumes that you have [phantomjs](https://www.npmjs.com/package/phantomjs) installed. However, note that [doo](https://github.com/bensu/doo) can be configured to run cljs.test in many other JS environments.

## Production Build

To compile CLJS to JS:

```
lein clean
lein cljsbuild once min
```
