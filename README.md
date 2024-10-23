# reagent-keybindings

Easy key bindings for your Reagent application.

You might have a sweet menus, but power users will want keyboard shortcuts!

## Install
Add this to your project dependencies:

[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.frozenlock/reagent-keybindings.svg)](https://clojars.org/org.clojars.frozenlock/reagent-keybindings)


## Usage

Include the `keyboard-listener` component in your document:

```clj
(defn main-page []
	[:div
	  [:h1 "Page title"]
	  [:span "Some content"]
	  [kb/keyboard-listener]]) ;  <-------
```

Now you can add key bindings as easily as adding a new component :

```clj
[kb/kb-action "ctrl-a" #(prn "Print this cool message!")]
```

You can also automatically overwrite a key binding by mounting another component :

```clj
[kb/kb-action "ctrl-a" #(prn "initial shortcut overwritten!")]
```

When the component is unmounted, any previous key binding is restored.
This allows you to temporarily give the users new shortcuts without
all the hassle.

### Propagation and Default Prevention

All key bindings will automatically prevent the default action
`.preventDefault` and stop the event propagation `.stopPropagation`.


Because key bindings often implies a more 'serious' web application, we
disable the following browser shortcuts automatically (if allowed by
the browser) :

- `ctrl-r` We don't want the user to reload by accident;
- `ctrl-s` Don't save the HTML file.

You can override this behavior by clearing the `preventing-default-keys` atom.


### Key Bindings Deactivation

It's possible to completely deactivate all the key bindings by
mounting the `deactivate-kb-shortcuts` component. 

A typical use is with a modal in which the user needs to type text in
a field.

### Keyboard State

You can access the keyboard state with the Reagent ratom
`keyboard-state`. The map contains the 3 main modifiers state
(`shift`, `ctrl` and `alt`) and the character keycode for the normal
key pressed.


## License

Copyright © 2023 Christian Fortin

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
