#!/opt/homebrew/bin/bash

declare -A MODULES
MODULES=(
    ["ktcloud-msa-inventory-service"]="inventory-service inventory inventory-event"
    ["ktcloud-msa-user-api-gateway"]="user-api-gateway"
    ["ktcloud-msa-order-service"]="order-service order"
    ["ktcloud-msa-product-service"]="product-service product"
    ["ktcloud-msa-common"]="common"
    ["ktcloud-msa-client-redis"]="client-redis"
)

COMMON_FILES=("gradlew" "gradlew.bat" "gradle" "build.gradle.kts" ".gitignore")

GH_USER="kanei0415"

OUTPUT_DIR="../ktcloud-msa-services"

mkdir -p "$OUTPUT_DIR"

for REPO in "${!MODULES[@]}"; do
    SUBMODULE_LIST=(${MODULES[$REPO]})

    WORK_PATH="$OUTPUT_DIR/$REPO"

    mkdir -p "$WORK_PATH"

    for FILE in "${COMMON_FILES[@]}"; do
        if [ -e "$FILE" ]; then
            cp -R "$FILE" "$WORK_PATH/"
        fi
    done

    for SUBMODULE in "${SUBMODULE_LIST[@]}"; do
        if [ -d "$SUBMODULE" ]; then
            cp -R "$SUBMODULE" "$WORK_PATH/"
        fi
    done

    cd "$WORK_PATH" || exit

    git init
    git add .
    git commit -m "Split modules [${SUBMODULE_LIST[*]}] to $REPO"

    if ! gh repo view "$GH_USER/$REPO" >/dev/null 2>&1; then
        gh repo create "$GH_USER/$REPO" --public --source=. --remote=origin --push
    else
        if ! git remote | grep origin > /dev/null; then
            git remote add origin "https://github.com/$GH_USER/$REPO.git"
        fi
        git branch -M main
        git push -u origin main --force
    fi

    cd - > /dev/null
done