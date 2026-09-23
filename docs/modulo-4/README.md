# Módulo 4 – Traer un fix específico de `main` a `feature/new-login`

**Respuesta:** `git cherry-pick -x <sha-del-fix>` estando en `feature/new-login`.

```bash
git fetch origin
git log origin/main --oneline --grep="security"   # identificar el commit exacto del fix
git switch feature/new-login
git cherry-pick -x <sha>                          # -m 1 si el fix llegó como merge commit de un PR
# resolver conflictos si los hay -> git add <archivos> -> git cherry-pick --continue
./mvnw verify                                     # validar que la feature sigue funcionando con el fix
git push origin feature/new-login                 # sin --force: solo se agregó un commit
```

`cherry-pick` aplica **solo el parche** de ese commit como un commit nuevo en la rama actual; `merge` o `rebase` de `main`
traerían también todos los demás cambios. `-x` agrega al mensaje la referencia `(cherry picked from commit …)` para mantener
la trazabilidad.

## Evidencia

[`cherry-pick-demo.sh`](cherry-pick-demo.sh) reproduce el escenario completo en un repositorio temporal: rama de feature,
fix de seguridad en `main` entre otros cambios, cherry-pick solo del fix, el caso del merge commit (`-m 1`) y la integración
final de la feature en `main` sin conflictos. La salida real está en [`salida-demo.txt`](salida-demo.txt).

```bash
bash docs/modulo-4/cherry-pick-demo.sh
```

La justificación completa, los casos especiales y las alternativas descartadas están en el PDF de la prueba.
