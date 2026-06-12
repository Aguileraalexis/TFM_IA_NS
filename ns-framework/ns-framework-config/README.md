# ns-framework-config

Este modulo gestiona configuracion externa para enlazar acciones simbolicas con implementaciones concretas.

## Para que sirve

- Cargar archivos JSON con bindings de acciones.
- Registrar y exponer esos bindings para el ejecutor de acciones.
- Desacoplar la definicion del dominio PDDL de los detalles de infraestructura.

## Que contiene

- `src/main/java/com/tesis/nsframework/config/binding/`: modelos de configuracion (`ActionBinding`, `ActionBindingsFile`).
- `src/main/java/com/tesis/nsframework/config/loader/`: carga y registro (`JsonActionBindingLoader`, `ActionBindingRegistry`).

## Dependencias principales

- `ns-framework-core`
- `jackson-databind`

