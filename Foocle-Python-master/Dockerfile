FROM python:3.11-slim

# 시간대
ENV TZ=Asia/Seoul \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app/ai

# 시스템 패키지 설치 (+ ffmpeg)
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    default-libmysqlclient-dev \
    pkg-config \
    build-essential \
    libssl-dev \
    libgl1 \
    libglib2.0-0 \
    tzdata \
    ffmpeg \
    && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone \
    && rm -rf /var/lib/apt/lists/*

# 파이썬 의존성 설치
COPY requirements.txt .
RUN python -m pip install -U pip setuptools wheel && \
    pip install --prefer-binary -r requirements.txt

# 앱 복사
COPY . .

# 실행
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "5000"]
