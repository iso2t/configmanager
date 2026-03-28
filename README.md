# ConfigManager

![GitHub License](https://img.shields.io/github/license/iso2t/configmanager?style=for-the-badge)
![Maven metadata](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fmaven.iso2t.com%2Freleases%2Fcom%2Fiso2t%2FConfigManager%2Fmaven-metadata.xml&style=for-the-badge)

> A **simple**, **class-based** configuration manager for Java — focused on JSON and JSON5.

ConfigManager lets you define your app’s settings as plain Java classes—no boilerplate, no hand-rolled parsers.  
Annotate with `@Config`, use `@Comment` for inline docs, and use type-safe value wrappers like `StringValue`, `IntegerValue`, or `ListValue`.  
Under the hood, it uses Jackson for parsing and serializing, supporting JSON5-style features like unquoted keys and comments.

---

## Table of Contents

- [Features](#features)
- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Installation](#installation)
- [Usage](#usage)
    - [Define a Config Class](#define-a-config-class)
    - [Load & Save](#load--save)
- [Advanced](#advanced)
    - [Default Values & Comments](#default-values--comments)
    - [Nested Containers](#nested-containers)
    - [Value Wrappers](#value-wrappers)
- [API Reference](#api-reference)
- [Contributing](#contributing)
- [License](#license)

---

## Features

- **Annotation-driven** – `@Config`, `@Comment`
- **JSON5 support** – unquoted keys, comments, trailing commas
- **Nested containers** – group related settings in sub-classes
- **Value wrappers** – `ConfigValue<T>`, `ListValue<T>`, `EnumValue<E>`, and primitive wrappers
- **Zero boilerplate** – defaults from field initializers
- **Human-friendly** – inline comments, pretty-printed output

---

## Getting Started

### Prerequisites

- Java 8+
- Jackson Core & Databind 2.18+

### Installation

**Gradle**
```kotlin
repositories {
    maven { url = uri("https://maven.iso2t.com/releases") }
}

dependencies {
    implementation("com.iso2t.configmanager:configmanager:1.0.0")
}
```

**Maven**

```xml
<repository>
  <id>iso2t</id>
  <url>https://maven.iso2t.com/releases</url>
</repository>

<dependency>
  <groupId>com.iso2t.configmanager</groupId>
  <artifactId>configmanager</artifactId>
  <version>1.0.0</version>
</dependency>
```

---

## Usage

### Define a Config Class

```java
import com.iso2t.configmanager.annotations.*;
import com.iso2t.configmanager.value.wrappers.BooleanValue;
import com.iso2t.configmanager.value.wrappers.IntegerValue;
import com.iso2t.configmanager.value.wrappers.StringValue;

@Config(name = "server_config")
public class ServerConfig {
  @Comment("Server port")
  public IntegerValue port = new IntegerValue(8080);

  @Comment("Enable verbose logging")
  public BooleanValue verbose = new BooleanValue(false);

  @Comment("Server name")
  public StringValue serverName = new StringValue("MyServer");
}
```

### Load & Save

```java
import com.iso2t.configmanager.manager.ConfigManager;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {
        // Create the manager
        ConfigManager<ServerConfig> manager = new ConfigManager<>(ServerConfig.class, Paths.get("config.json5"));
        
        // Load (creates a new instance with defaults if file doesn't exist)
        ServerConfig config = manager.load();
        
        System.out.println("Port: " + config.port.get());
        
        // Save back to file
        manager.save(config);
    }
}
```

---

## Advanced

### Default Values & Comments

* Field initializers become defaults on first load.
* Use `@Comment({"Line 1", "Line 2"})` to embed notes above each entry.

### Nested Containers

```java
@Config
public class AppConfig {
    public static class Db {
        @Comment("JDBC URL")     
        public StringValue url  = new StringValue("jdbc:h2:mem:test");
        @Comment("DB user")      
        public StringValue user = new StringValue("sa");
    }
    
    @Comment("Database settings")
    public Db database = new Db();
}
```

### Value Wrappers

* **Primitive Wrappers**: `IntegerValue`, `LongValue`, `DoubleValue`, `FloatValue`, `BooleanValue`, `ShortValue`, `ByteValue`
* **String**: `StringValue`
* **List**: `ListValue<T>`
* **Enums**: `EnumValue<E>`

---

## API Reference

### `ConfigManager<T>`

```java
public final class ConfigManager<T> {
  public ConfigManager(Class<T> type, Path file);
  
  public T load() throws IOException, IllegalAccessException;
  public void save(T config) throws IOException, IllegalAccessException;
}
```

---

## Contributing

1. Fork the repo
2. Create a branch (`git checkout -b feat/YourFeature`)
3. Commit your changes (`git commit -m "Add feature"`)
4. Push & open a PR

---

## License

This project is MIT-licensed. See the [LICENSE](LICENSE) file for details.

