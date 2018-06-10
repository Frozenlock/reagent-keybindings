# reagent-keybindings

Easy keybindings for your Reagent application.

You might have a sweet menus, but power users will want keybindings!

## Install
Add this to your project dependencies:

[![Clojars Project](http://clojars.org/org.clojars.frozenlock/reagent-keybindings/latest-version.svg)](http://clojars.org/org.clojars.frozenlock/reagent-keybindings)


## Usage

Include the `keyboard-listener` component in your document:

```clj
(defn main-page []
	[:div
	  [:h1 "Page title"]
	  [:span "Some content"]
	  [kb/keyboard-listener]]) ;  <-------
```

Now you can add keybindings as easily as adding a new component :

```clj
[kb/kb-action "ctrl-a" #(prn "Print this cool message!")]
```

You can also automatically overwrite a keybinding by mounting another component :

```clj
[kb/kb-action "ctrl-a" #(prn "initial shortcut overwritten!")]
```

When the component is unmounted, any previous keybinding is restored.
This allows you to temporarely give the users new shortcuts without
all the assle.

### Propagation and Default Prevention

All keybindings will automatically prevent the default action
`.preventDefault` and stop the event propagation `.stopPropagation`.


Because keybindings often implies a more 'serious' web application, we
disable the following browser shortcuts automatically (if allowed by
the browser) :

- `ctrl-r` We don't want the user to reload by accident;
- `ctrl-s` Don't save the HTML file.

You can override this behavior by clearing the `preventing-default-keys` atom.


### Keyboard State

You can access the keyboard state with the Reagent ratom
`keyboard-state`. The map contains the 3 main modifiers state
(`shift`, `ctrl` and `alt`) and the character keycode for the normal
key pressed.


## License

Copyright © 2018 Christian Fortin

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
