#!/usr/bin/env bash
# E2E Fase 1 EC3: API real + BD real. Requiere backend en :8081 y gestor-tareas-db.
set -u
API=http://localhost:8081/api
PASS=0; FAIL=0
check(){ if [ "$2" = "$3" ]; then echo "PASS  $1 ($2)"; PASS=$((PASS+1)); else echo "FAIL  $1 esperado=$3 obtenido=$2"; FAIL=$((FAIL+1)); fi; }
login(){ curl -s -X POST $API/auth/login -H 'Content-Type: application/json' -d "{\"email\":\"$1\",\"password\":\"Gestor2026!\"}" | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])'; }
post(){ curl -s -o /tmp/e2e_body.json -w '%{http_code}' -X POST "$API$2" -H "Authorization: Bearer $1" -H 'Content-Type: application/json' -d "$3"; }
field(){ python3 -c "import json;print(json.load(open('/tmp/e2e_body.json'))$1)"; }
sql(){ docker exec gestor-tareas-db psql -U postgres -d gestor_tareas -tAc "$1"; }

JEFE=$(login jefe@gestor.local); ENC=$(login encargado1@gestor.local)
STAMP=$(date +%s)
check "login jefe/encargado" "$([ -n "$JEFE" ] && [ -n "$ENC" ] && echo ok)" ok

code=$(post $JEFE /projects "{\"name\":\"E2E Fase1 $STAMP\",\"description\":null,\"managerUserId\":2,\"budgetBob\":5000.00,\"estimatedDurationMinutes\":4800,\"reason\":null}")
check "crear proyecto (Jefe)" $code 201; PID=$(field '["projectId"]')
code=$(post $ENC /projects/$PID/stages '{"name":"Obra gruesa","description":null}'); check "crear etapa" $code 201; SID=$(field '["stageId"]')
code=$(post $ENC /stages/$SID/activities '{"name":"Cimentación","description":null}'); check "crear actividad" $code 201; AID=$(field '["activityId"]')

# EC3-04: el body que ahora envía task-form (1.5 h -> 90 min, presupuesto en su campo)
code=$(post $ENC /tasks "{\"activityId\":$AID,\"title\":\"Vaciado de zapatas\",\"priority\":\"ALTA\",\"allocatedBudgetBob\":250.50,\"estimatedDurationMinutes\":90}")
check "EC3-04 crear tarea" $code 201; TID=$(field '["taskId"]')
check "EC3-04 respuesta duración (min)" "$(field '["estimatedDurationMinutes"]')" 90
check "EC3-04 respuesta presupuesto" "$(field '["allocatedBudgetBob"]')" 250.5
check "EC3-04 BD task" "$(sql "select estimated_duration_minutes||'|'||allocated_budget_bob from task_manager.task where task_id=$TID")" "90|250.50"
code=$(post $ENC /tasks "{\"activityId\":$AID,\"title\":\"Neg\",\"priority\":\"MEDIA\",\"allocatedBudgetBob\":0,\"estimatedDurationMinutes\":-1}"); check "BVA duración -1 rechazada" $code 400
code=$(post $ENC /tasks "{\"activityId\":$AID,\"title\":\"Neg\",\"priority\":\"MEDIA\",\"allocatedBudgetBob\":-0.01,\"estimatedDurationMinutes\":0}"); check "BVA presupuesto -0.01 rechazado" $code 400

# EC3-02 / EC3-03: motivo de asignación
code=$(post $ENC /tasks/$TID/assignments '{"workerUserId":4,"reason":"  Refuerzo de cuadrilla  "}')
check "EC3-02 asignar Personal" $code 200
check "EC3-02 respuesta motivo" "$(field '["reason"]')" "Refuerzo de cuadrilla"
check "EC3-02 motivo persistido en BD" "$(sql "select reason from task_manager.task_assignment where task_id=$TID and worker_user_id=4")" "Refuerzo de cuadrilla"
LIST=$(curl -s $API/tasks/$TID/assignments -H "Authorization: Bearer $ENC" | python3 -c 'import sys,json;print(json.load(sys.stdin)[0]["reason"])')
check "EC3-02 listado devuelve motivo" "$LIST" "Refuerzo de cuadrilla"
code=$(post $ENC /tasks/$TID/assignments '{"workerUserId":3,"reason":null}'); check "motivo por defecto" $code 200
check "motivo por defecto en BD" "$(sql "select reason from task_manager.task_assignment where task_id=$TID and worker_user_id=3")" "Asignación de Personal"
R500=$(python3 -c 'print("a"*500)'); R501=$(python3 -c 'print("a"*501)')
code=$(post $ENC /tasks/$TID/assignments "{\"workerUserId\":1,\"reason\":\"$R501\"}"); check "EC3-03 BVA motivo 501 rechazado" $code 400
code=$(post $ENC /tasks/$TID/assignments "{\"workerUserId\":1,\"reason\":\"$R500\"}"); check "EC3-03 BVA motivo 500 aceptado" $code 200
check "EC3-03 longitud 500 en BD" "$(sql "select length(reason) from task_manager.task_assignment where task_id=$TID and worker_user_id=1")" 500
code=$(post $ENC /tasks/$TID/assignments '{"workerUserId":4,"reason":"otra"}'); check "duplicidad rechazada" $code 400
code=$(post $ENC /tasks/$TID/assignments '{"workerUserId":5,"reason":null}'); check "trabajador inactivo rechazado" $code 400

echo "proyecto E2E creado: $PID (tarea $TID)"
echo "TOTAL: $PASS PASS / $FAIL FAIL"
