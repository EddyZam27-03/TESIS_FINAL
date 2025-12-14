# 🏆 CÓMO FUNCIONAN LOS LOGROS EN LA APLICACIÓN

## 📋 Resumen

Los logros (achievements) en la aplicación **NO se cargan automáticamente al iniciar la app**. En su lugar, se **desbloquean automáticamente** cuando el usuario cumple ciertas condiciones durante sus actividades.

## 🔄 Flujo de Funcionamiento

### 1. **Carga Inicial de Logros**

Los logros **NO aparecen** en la lista hasta que:
- El usuario haya realizado alguna actividad (practicar gestos)
- Se haya actualizado el progreso del usuario
- Se haya ejecutado la verificación automática de logros

### 2. **Detección Automática de Logros**

Los logros se verifican y desbloquean automáticamente en los siguientes momentos:

#### ✅ Después de Actualizar Progreso
- Cuando el usuario practica un gesto y obtiene un porcentaje
- Cuando se actualiza el estado de un gesto (pendiente → aprendido)
- Cuando se sincroniza el progreso con el servidor

#### ✅ Después de Actividades Específicas
- Cuando un estudiante envía una solicitud a un docente
- Cuando se acepta una relación docente-estudiante
- Cuando se completa un gesto al 100%

### 3. **Verificación Automática**

La función `LogroRepository.verificarYDesbloquearLogros()` se ejecuta automáticamente después de:
- Cada actualización de progreso en `ProgresoRepository`
- Cada sincronización de datos
- Cada práctica de gesto completada

## 📊 Categorías de Logros Implementadas

### 1. **📘 Progreso Básico**
- ✅ **Primer gesto aprendido** - Se desbloquea cuando el usuario aprende su primer gesto
- ✅ **10 gestos aprendidos** - Se desbloquea cuando el usuario aprende 10 gestos
- ✅ **25 gestos aprendidos** - Se desbloquea cuando el usuario aprende 25 gestos
- ✅ **50 gestos aprendidos** - Se desbloquea cuando el usuario aprende 50 gestos
- ✅ **100 gestos aprendidos** - Se desbloquea cuando el usuario aprende 100 gestos

### 2. **🎯 Rendimiento**
- ✅ **Perfeccionista** - Se desbloquea cuando el promedio de progreso es ≥ 90%
- ✅ **Estudiante dedicado** - Se desbloquea cuando el promedio de progreso es ≥ 70%
- ✅ **100% en un gesto** - Se desbloquea cuando el usuario obtiene 100% en cualquier gesto
- ✅ **10 gestos al 80%** - Se desbloquea cuando el usuario tiene 10 gestos con ≥ 80%
- ✅ **20 gestos al 80%** - Se desbloquea cuando el usuario tiene 20 gestos con ≥ 80%

### 3. **⭐ Participación y Comunidad**
- ✅ **Enviar primera solicitud** - Se desbloquea cuando el estudiante envía su primera solicitud a un docente
- ✅ **Vincularse con un docente** - Se desbloquea cuando se acepta una relación docente-estudiante

## 🔍 Cómo Verificar si los Logros Están Funcionando

### Para Desarrolladores:

1. **Verificar en la Base de Datos:**
   ```kotlin
   // Ver logros desbloqueados
   val logros = usuarioLogroDao.getLogrosByUsuario(idUsuario)
   ```

2. **Verificar en el Código:**
   - `LogroRepository.verificarYDesbloquearLogros()` se llama desde:
     - `ProgresoRepository.updateProgreso()`
     - `ProgresoRepository.syncProgreso()`
     - Después de cada práctica de gesto

3. **Logs de Debug:**
   - Los logros desbloqueados se registran en los logs
   - Se muestran notificaciones (Toast + Push si está habilitado)

### Para Usuarios:

1. **Los logros aparecerán automáticamente** cuando:
   - Practiques gestos y obtengas progreso
   - Aprendas nuevos gestos
   - Alcances porcentajes específicos
   - Te vincules con un docente

2. **Para ver tus logros:**
   - Ve a la pantalla "Logros" desde el menú inferior
   - Los logros desbloqueados aparecerán con su fecha de obtención
   - Los logros pendientes también aparecerán (pero sin fecha)

## ⚠️ Notas Importantes

### ¿Por qué no veo logros al iniciar la app?

**Esto es normal.** Los logros solo se cargan cuando:
1. El usuario ha realizado actividades
2. Se ha actualizado el progreso
3. Se ha ejecutado la verificación automática

### ¿Cuándo se desbloquean los logros?

Los logros se desbloquean **automáticamente** cuando:
- Practicas gestos y obtienes progreso
- Aprendes nuevos gestos (estado cambia a "aprendido")
- Alcanzas porcentajes específicos
- Te vinculas con un docente
- Envías solicitudes a docentes

### ¿Los logros se sincronizan con el servidor?

Sí, los logros desbloqueados se sincronizan automáticamente con el servidor si hay conexión a internet. Si no hay conexión, se guardan localmente y se sincronizan cuando haya conexión.

## 🐛 Solución de Problemas

### Si no ves ningún logro:

1. **Verifica que hayas realizado actividades:**
   - Practica algunos gestos
   - Aprende al menos un gesto
   - Verifica tu progreso

2. **Verifica la base de datos:**
   - Los logros deben estar en la tabla `logros`
   - Los logros desbloqueados deben estar en `usuario_logros`

3. **Verifica los logs:**
   - Busca errores en `LogroRepository`
   - Verifica que `verificarYDesbloquearLogros()` se esté ejecutando

### Si los logros no se desbloquean:

1. **Verifica las condiciones:**
   - Asegúrate de cumplir las condiciones específicas de cada logro
   - Por ejemplo, "10 gestos aprendidos" requiere exactamente 10 gestos con estado "aprendido"

2. **Verifica la sincronización:**
   - Los logros se verifican después de actualizar el progreso
   - Si no se actualiza el progreso, los logros no se verificarán

## 📝 Código Relevante

### Archivos Clave:
- `LogroRepository.kt` - Lógica de verificación y desbloqueo
- `ProgresoRepository.kt` - Llama a la verificación después de actualizar progreso
- `LogrosFragment.kt` - Muestra los logros al usuario
- `LogroDetailFragment.kt` - Muestra el detalle de un logro

### Función Principal:
```kotlin
LogroRepository.verificarYDesbloquearLogros(idUsuario: Int)
```

Esta función:
1. Obtiene todos los logros disponibles
2. Obtiene el progreso del usuario
3. Verifica cada logro contra las condiciones
4. Desbloquea los logros que cumplan las condiciones
5. Sincroniza con el servidor si hay conexión
6. Muestra notificaciones al usuario

---

**En resumen:** Los logros funcionan automáticamente cuando realizas actividades. No aparecen al iniciar la app porque se desbloquean dinámicamente según tu progreso y acciones.

