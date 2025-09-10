#!/bin/bash

# 初始訓練模型
docker-compose run rasa rasa train
# 啟動服務
docker-compose up
