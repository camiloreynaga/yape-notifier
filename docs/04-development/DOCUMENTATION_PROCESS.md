# Proceso de Documentación - Comando "documentar"

Guía de proceso para revisar, consolidar y organizar la documentación del proyecto.

---

## 🎯 Objetivo

Cuando se ejecuta el comando **"documentar"**, se debe realizar una revisión completa de la documentación siguiendo criterios estandarizados para mantener la calidad, organización y coherencia de todos los documentos `.md` del proyecto.

---

## 📋 Criterios de Documentación

### 1. Estructura de Organización

La documentación debe estar organizada en `docs/` siguiendo esta estructura:

```
docs/
├── 01-getting-started/     # Inicio rápido, instalación, setup
├── 02-deployment/          # Despliegue, Docker, producción
├── 03-architecture/        # Arquitectura técnica, decisiones de diseño
├── 04-development/         # Desarrollo, testing, workflow
├── 04-security/            # Seguridad, multi-tenant
├── 05-features/            # Funcionalidades y features
├── 06-operations/          # Operaciones, monitoreo, troubleshooting
└── 07-reference/           # Referencia: bugs, roadmap, changelog
```

### 2. Nomenclatura de Archivos

- **Formato**: `UPPER_SNAKE_CASE.md` (ej: `DEVICE_LINKING_GUIDE.md`)
- **Idioma**: Inglés (excepto casos específicos justificados)
- **Descriptivo**: El nombre debe indicar claramente el contenido
- **Sin duplicados**: No debe haber archivos con contenido similar

### 3. Ubicación de Archivos

- **Raíz del proyecto**: Solo `README.md` principal
- **Documentación activa**: Todo en `docs/` organizado por categorías
- **Documentación obsoleta**: Mover a `ARCHIVE/` con fecha si es relevante
- **Documentación temporal**: Evaluar si debe consolidarse o archivarse

### 4. Contenido y Calidad

- **Referencias cruzadas**: Agregar sección "Referencias relacionadas" al inicio
- **Estructura clara**: Headers jerárquicos, secciones bien definidas
- **Actualizado**: Información relevante y actual
- **Sin duplicaciones**: Consolidar contenido similar en un solo documento

---

## 🔄 Proceso de Revisión (Comando "documentar")

### Paso 1: Identificar Nuevos Archivos

```bash
# Buscar archivos .md en la raíz (excepto README.md)
# Buscar archivos .md en docs/ que no estén listados en docs/README.md
```

**Acciones:**
- Listar todos los archivos `.md` en la raíz del proyecto
- Comparar con archivos listados en `docs/README.md`
- Identificar archivos nuevos o no documentados

### Paso 2: Clasificar Archivos

Para cada archivo encontrado:

1. **Evaluar contenido**: ¿Qué información contiene?
2. **Determinar categoría**: ¿A qué sección de `docs/` pertenece?
3. **Verificar duplicados**: ¿Existe contenido similar en otro archivo?
4. **Decidir acción**:
   - **Mover**: Si está en ubicación incorrecta
   - **Consolidar**: Si hay contenido duplicado o similar
   - **Archivar**: Si es obsoleto pero relevante históricamente
   - **Eliminar**: Si es completamente obsoleto o innecesario
   - **Actualizar**: Si necesita mejoras o correcciones

### Paso 3: Consolidar Contenido

Cuando hay archivos con contenido similar:

1. **Identificar el documento principal** (más completo o actualizado)
2. **Extraer información relevante** de documentos secundarios
3. **Integrar contenido** en el documento principal
4. **Actualizar referencias** en otros documentos
5. **Archivar o eliminar** documentos consolidados

### Paso 4: Actualizar Referencias

1. **Agregar referencias cruzadas** en documentos relacionados
2. **Actualizar `docs/README.md`** con nuevos archivos
3. **Corregir enlaces rotos** o referencias incorrectas
4. **Verificar rutas relativas** en todos los documentos

### Paso 5: Limpiar y Organizar

1. **Mover archivos** a ubicaciones correctas
2. **Renombrar** si es necesario (seguir nomenclatura)
3. **Eliminar duplicados** o archivos innecesarios
4. **Archivar** documentos obsoletos en `ARCHIVE/`

### Paso 6: Verificar Calidad

1. **Revisar estructura** de cada documento
2. **Verificar formato** (headers, listas, código)
3. **Comprobar enlaces** y referencias
4. **Validar consistencia** de estilo y tono

---

## 📝 Checklist de Revisión

### Para Cada Archivo .md

- [ ] ¿Está en la ubicación correcta según su contenido?
- [ ] ¿Sigue la nomenclatura estándar (UPPER_SNAKE_CASE)?
- [ ] ¿Tiene referencias cruzadas a documentos relacionados?
- [ ] ¿Está listado en `docs/README.md`?
- [ ] ¿No hay contenido duplicado en otros archivos?
- [ ] ¿La información está actualizada?
- [ ] ¿Los enlaces y referencias funcionan correctamente?
- [ ] ¿Tiene una estructura clara y bien organizada?

### Para la Estructura General

- [ ] ¿No hay archivos .md en la raíz (excepto README.md)?
- [ ] ¿Todos los archivos en `docs/` están organizados por categorías?
- [ ] ¿`docs/README.md` está actualizado y sin duplicaciones?
- [ ] ¿Las referencias cruzadas están actualizadas?
- [ ] ¿No hay archivos obsoletos en ubicaciones activas?

---

## 🎯 Ejemplos de Acciones

### Caso 1: Archivo Nuevo en Raíz

**Archivo**: `NUEVO_FEATURE.md` en la raíz

**Acción**:
1. Leer contenido
2. Determinar categoría (ej: `05-features/`)
3. Mover a `docs/05-features/NUEVO_FEATURE.md`
4. Agregar a `docs/README.md`
5. Agregar referencias cruzadas en documentos relacionados

### Caso 2: Contenido Duplicado

**Archivos**: 
- `docs/05-features/DEVICE_LINKING.md`
- `docs/05-features/DEVICE_LINKING_DETAILED.md` (similar)

**Acción**:
1. Identificar documento principal (más completo)
2. Consolidar información relevante
3. Actualizar referencias en otros documentos
4. Eliminar o archivar documento duplicado
5. Actualizar `docs/README.md`

### Caso 3: Archivo Obsoleto

**Archivo**: `docs/02-deployment/OLD_DEPLOYMENT.md` (método antiguo)

**Acción**:
1. Verificar si tiene información histórica relevante
2. Si es relevante: Mover a `ARCHIVE/OLD_DEPLOYMENT.md`
3. Si no es relevante: Eliminar
4. Actualizar referencias en otros documentos
5. Actualizar `docs/README.md`

### Caso 4: Referencias Rotas

**Problema**: Documento A referencia a Documento B que fue movido

**Acción**:
1. Buscar todas las referencias al documento movido
2. Actualizar rutas relativas
3. Verificar que todos los enlaces funcionen

---

## 🔍 Comandos Útiles

### Buscar Archivos .md

```bash
# En la raíz (excepto README.md)
find . -maxdepth 1 -name "*.md" ! -name "README.md"

# En docs/
find docs/ -name "*.md"
```

### Buscar Referencias

```bash
# Buscar referencias a un archivo específico
grep -r "NOMBRE_ARCHIVO" docs/

# Buscar enlaces rotos
grep -r "\.md" docs/ | grep -v "http"
```

### Verificar Estructura

```bash
# Listar estructura de docs/
tree docs/ -I 'node_modules|vendor'
```

---

## 📚 Referencias

- [docs/README.md](../README.md) - Índice completo de documentación
- [ESTANDARES_Y_CALIDAD.md](../07-reference/ESTANDARES_Y_CALIDAD.md) - Estándares de calidad
- [WORKFLOW.md](WORKFLOW.md) - Flujo de trabajo de desarrollo

---

## ✅ Resultado Esperado

Después de ejecutar el comando "documentar":

1. ✅ Todos los archivos .md están en ubicaciones correctas
2. ✅ No hay duplicaciones de contenido
3. ✅ Todas las referencias están actualizadas
4. ✅ `docs/README.md` refleja la estructura actual
5. ✅ La documentación está organizada y accesible
6. ✅ Los archivos obsoletos están archivados o eliminados

---

**Última actualización:** 2025-01-21





