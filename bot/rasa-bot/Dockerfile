FROM python:3.9-slim

WORKDIR /app

# 更新 pip 並安裝依賴
COPY requirements.txt ./
RUN pip install --upgrade pip && pip install -r requirements.txt

# 拷貝內容
COPY . .

# 若需要在建置時預先訓練模型，請取消註解以下
RUN rasa train

# 啟動指令
CMD ["rasa", "run", "--enable-api", "--cors", "*", "--debug"]
