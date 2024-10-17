# Wikibase Chile

## Descripción

**WikibaseManager** es una herramienta para gestionar entidades en una instancia de Wikibase mediante Java. Permite la creación, actualización y consulta de propiedades, ítems y declaraciones en una base de datos Wikibase. Esta aplicación está diseñada para automatizar la carga y gestión de datos educativos y otros tipos de información en una instancia de Wikibase.

## Funcionalidades

- **Creación de propiedades**: Define nuevas propiedades en Wikibase para describir entidades y valores específicos, como coordenadas, niveles de enseñanza, estados, etc.
- **Gestión de ítems**: Automatiza la creación de ítems como regiones, comunas, establecimientos y más, permitiendo la inclusión de alias y descripciones.
- **Registro de ejecuciones**: Registra el tiempo de ejecución y la cantidad de líneas procesadas en un archivo CSV para fines de monitoreo y análisis de rendimiento.
- **Consulta de entidades**: Verifica la existencia de entidades por ID, etiqueta o alias, y realiza consultas para gestionar las relaciones entre ellas.

## Estructura del Proyecto

### Paquetes y Clases Principales

- **`wikibase.WikibaseManager`**: Clase principal que maneja las operaciones en Wikibase. Implementa métodos para autenticación, creación y manipulación de entidades, y manejo de tokens CSRF.
- **`wikibase.VariablePosition`**: Clase auxiliar para manejar variables y posiciones, útil para procesar y mapear datos de entrada.
- **`wikibase.ExecutionLogger`**: Clase estática para registrar la duración de las ejecuciones y las líneas leídas en un archivo CSV.
- **`wikibase_inicializador`**: Clase de inicialización que configura las propiedades e ítems básicos en la instancia de Wikibase. Útil para una configuración inicial rápida del entorno.

### Archivos Generados

- **`execution.csv`**: Archivo de registro que almacena los tiempos de ejecución y la cantidad de líneas leídas durante la operación del programa.

## Uso

### Requisitos Previos

- Java 8+
- Apache Maven
- Acceso a una instancia de Wikibase con permisos para crear y modificar ítems y propiedades.
- Un archivo CSV con datos de entrada para procesar y cargar en Wikibase.

### Configuración

1. Clona el repositorio:

    ```bash
    git clone https://github.com/tu_usuario/WikibaseManager.git
    cd WikibaseManager
    ```

2. Configura las dependencias (si estás usando Maven):

    ```bash
    mvn clean install
    ```

3. Compilar:
    ```bash
    mvn clean compile assembly:single
    ```
4. Modifica la clase `wikibase_inicializador` si necesitas ajustar la creación inicial de propiedades e ítems.

### Ejecución

Para ejecutar el programa:

```bash
java -jar WikibaseManager.jar <usuario> <contraseña> <archivo.csv>
```

### Contacto

Para cualquier consulta o problema, puedes contactar al desarrollador principal en mavb2001@gmail.com
