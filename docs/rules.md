# <u> Methods </u>

To declare a method, use the `meth` keyword.
The syntax should look like the following:
```
meth example() {
    [contents]
}
```

A method with arguments would look like this:
```
meth example(arg_a, arg_b) {
    [contents]
}
```

You can return a value from the method using the `return` keyword:
```
meth example() {
    return 5
}
```

You can also have single lined methods:
```
meth example() -> [contents]
```

Externally added methods can be called like this:
```
[library identifier]::[method name]()
```

An example, using the `std#println(any)` method would look like this:
`std::println([argument])`

# <u> Conditions </u>
You can check a condition with an `if` statement.

The syntax should look like the following:
```
if ([condition]) {
    [contents]
}
```

You can add additional expressions using `elseif` and `else`:
```
if ([condition]) {
    [contents]
} elseif ([condition 2]) {
    [contents]
} else {
    [contents]
}
```

It's worth noting that if you have `elseif` or `else`, they have to be on the same line as
the last `}`.

# <u> Loops </u>
You can run loops through either `for` or `while`.

### <u> For Loops </u>
The syntax should look like this:
```
for ([index name] = [start] > [end]) {
    [contents]
}
```

`[index name]` defines the current index of the number. Normally, this should be set to `i`.

You can define the step as well, simply add `step = [value]` after the range:
```
for ([index name] = [start] > [end] step = [step]) {
    [contents]
}
```

### <u> While Loops </u>
The syntax should look like this:
```
while ([condition]) {
    [contents]
}
```

## <u> Loop Keywords </u>
Both types of loop have keywords that can be used.<br>
`continue` ignores the rest of the code after it and proceeds onto the next iteration of the loop.<br>
`break` ignores the rest of the code after it and ends the loop.

# <u> Variables </u>
You can declare variables by using either the `mut` or `const` keywords.

`mut` is short for mutable, and these can be changed whenever. <br>
`const` is short for constant, and these can never be changed.

The syntax should look like this:
`[finality] [name] = [expression]`

Variables are only accessible within the current scope.

This means that any variables declared within braces will only
be available in those braces. For example:

```
if (true) {
    mut a = true
    
    // accessible here
    std::println(a)
}

// will throw an error, not accessible here.
std::println(a)
```

# <u> Comments </u>
You can declare comments by using `//` before the comment.
The syntax should look like this:

```
// this is a comment

mut a = 5 // a comment can be after anything
```

# <u> Structs </u>
You can define structs with the `struct` keyword.
The syntax should look like this:

```
struct [name] {
    [properties]
}
```

### <u> Implementation </u>
If you want to implement methods into a struct, you can use the `impl` keyword.
The syntax should look like this:

```
impl [struct name] {
    [contents]
}
```

The struct needs to have been declared before the implementation, otherwise errors will be thrown.

The `this` variable is reserved for an instance of the struct, so you can modify or get the properties.

Here's an example (note - "Lliw" is Welsh for colo(u)r, to avoid arguments):

```
struct Lliw {
    red,
    green,
    blue
}

impl Lliw {
    fun average() {
        return (this::red + this::green + this::blue) / 3
    }
}

meth main(args) {
    const lliw = Lliw(32, 178, 254)
    
    // access raw value
    std::println(lliw::red)
    
    // access method
    std::println(lliw::average())
}
```

# <u> Importing </u>
You can import structs and functions from other files using the `use` keyword.
The syntax should look like this:

```
use "[path]"
```

So, if I wanted to access the structs and methods inside the `secondary` class, I would use:

```
use "secondary"
```