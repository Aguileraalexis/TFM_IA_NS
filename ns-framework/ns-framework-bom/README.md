# ns-framework-bom

Este modulo publica un BOM (Bill of Materials) para consumir el framework con versiones alineadas.

## Para que sirve

- Centralizar versiones de los artefactos `com.tesis` del framework.
- Evitar declarar versiones manuales en cada dependencia al consumir desde otro proyecto.
- Mantener coherencia de versiones entre modulos.

## Que contiene

- `pom.xml` con `packaging` `pom`.
- Seccion `dependencyManagement` con todos los modulos del framework y librerias base.

## Uso rapido

En un proyecto consumidor, importa el BOM y luego declara dependencias sin version.

