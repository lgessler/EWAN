# ewan

This is an experimental port of a minimal feature-subset of [ELAN](https://tla.mpi.nl/tools/tla-tools/elan/) into the browser. I used a variety of tools I'm excited about, including [ClojureScript](https://clojurescript.org/),  [re-frame](https://github.com/Day8/re-frame), [PouchDB](https://pouchdb.com/), and [Material-UI](http://www.material-ui.com/).

**You can try the demo [here](https://lgessler.com/ewan/).**

I have stopped working on this project and don't expect it to ever be finished.

# Roadmap

☑️ Translate [EAF XSD](http://www.mpi.nl/tools/elan/EAFv3.0.xsd) into a Clojure [spec](https://clojure.org/guides/spec)

☑️ Import of ELAN files

☑️ Read-only display and playback of ELAN files

☑️ Offline operation

☑️ Export to ELAN files

☐ Support for most basic ELAN workflows

☐ Remote syncing of projects

☐ Live collaborative editing *à la* Google Docs

☐ Plugin API letting users write custom scripts in plain JavaScript

☐ Full ELAN functionality coverage

# Building

## Install Clojure and Leiningen

[Clojure](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools), [Leiningen](https://github.com/technomancy/leiningen)

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
