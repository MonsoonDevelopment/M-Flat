# <u> Standard Library </u>
Prefixed by `std`

<hr>

### `print(value)`
Takes in any value and prints it to the console.
Does <b>not</b> add a new line afterwards.

Example:
```
std::print("Hello, world!")
```

Output:
```
Hello, world!
```

<hr>

### `println(value)`
Takes in any value and prints it to the console.
Adds a new line afterwards.

Example:
```
std::print("Hello, world!")
```

Output:
```
Hello, world! [\n]
```

<hr>

### `input()`
#### Returns string
Requests input from the console. Returns a string of the data 
that was inputted.

Example:
```
const in = input()
std::println(in)
```

Input:
```
example
```

Output:
```
example
```

<hr>

### `inputMessage(message)`
#### Returns string
Requests input from the console, with a given message. Returns a string of the
data that was inputted.

Example:
```
const in = inputMessage("Input anything here > ")
std::println(in)
```

Input:
```
Input anything here > example
```

Output:
```
example
```

<hr>

### `add(list, value)`
Adds `value` to the given `list`.

Example:
```
const list = [1, 2, 3]
std::println(list)
std::add(list, 4)
std::println(list)
```

Output:
```
[1, 2, 3]
[1, 2, 3, 4]
```

<hr>

### `remove(list, index)`
Removes the element at the given `index` of the given `list`.

Example:
```
const list = [1, 2, 3]
std::println(list)
std::remove(list, 2)
std::println(list)
```

Output:
```
[1, 2, 3]
[1, 2]
```

<hr>

### `get(list, index)`
#### Returns value
Gets and returns element at the given `index` of the given `list`.

Example:
```
const list = [1, 2, 3]
std::println(std::get(list, 2))
```

Output:
```
3
```

<hr>

### `combineLists(list_a, list_b)`
Adds all the elements from `list_b` to `list_a`.

Example:
```
const list_a = [1, 2, 3]
const list_b = [4, 5, 6]
std::println(list_a)
std::println(list_b)
std::combineLists(list_a, list_b)
std::println(list_a)
```

Output:
```
[1, 2, 3]
[4, 5, 6]
[1, 2, 3, 4, 5, 6]
```

<hr>

### `isNumber(value)`
#### Returns number
Checks if the given `value` is an instance of a number.

Example:
```
const a = 5
const b = "example"

std::println(std::isNumber(a))
std::println(std::isNumber(b))
```

Output:
```
1
0
```

<hr>

### `isString(value)`
#### Returns number
Checks if the given `value` is an instance of a string.

Example:
```
const a = 5
const b = "example"

std::println(std::isString(a))
std::println(std::isString(b))
```

Output:
```
0
1
```

<hr>

### `isMethod(value)`
#### Returns number
Checks if the given `value` is an instance of a method.

Example:
```
const a = meth() -> std::println("hello, world!")
const b = "example"

std::println(std::isMethod(a))
std::println(std::isMethod(b))
```

Output:
```
1
0
```

<hr>

### `isList(value)`
#### Returns number
Checks if the given `value` is an instance of a list.

Example:
```
const a = [1, 2, 3]
const b = "example"

std::println(std::isList(a))
std::println(std::isList(b))
```

Output:
```
1
0
```

<hr>

### `stringToNumber(string)`
#### Returns number
Converts the given string to a number.

Example:
```
const a = "5"
const b = stringToNumber(a)

std::println(b + 5)
```

Output:
```
10
```

<hr>

### `stringToBool(value)`
#### Returns number
Converts the given string to binary.

Example:
```
const a = "true"

if std::stringToBool(a) {
    std::println("Example")
}
```

Output:
```
Example
```

<hr>

### `delete(value)`
Deletes a value from the current symbol table.

Example:
```
const a = 5
std::println(a)
std::delete(a)
std::println(a) // throws error
```

Output:
```
5
[error]
```

<hr>

### `exit(status)`
Exits the program with the given status code.

Example:
```
std::exit(0)
```

Output:
```
Process finished with exit code 0
```

<hr>

### `type(value)`
#### Returns string
Gets and returns the type of the value

Example:
```
std::println(std::type("example"))
```

Output:
```
string
```

<hr>

### `equalsIgnoreCase(a, b)`
#### Returns number
Checks if two strings are equal, case-insensitively.

Example:
```
std::println(std::equalsIgnoreCase("eXaMpLe", "ExAmPlE"))
```

Output:
```
1
```