import enum
import time
import os
from typing import List
from dotenv import load_dotenv
from google import genai
from google.genai import types
from PIL import Image
import io
from moviepy.video.VideoClip import TextClip
from moviepy.video.compositing.CompositeVideoClip import CompositeVideoClip
from moviepy.video.io.VideoFileClip import VideoFileClip
from numpy.random import random
from openai import OpenAI
import base64
import subprocess
import tempfile
load_dotenv()
client = genai.Client()
import uuid
import time


def generate_video(prompt: str, image_path: str):
    try:
        # 전용 디렉토리 생성
        import os
        output_dir = "generated_videos"
        if not os.path.exists(output_dir):
            os.makedirs(output_dir)
        
        with open(image_path, "rb") as f:
            image_bytes = f.read()
        image = types.Image(image_bytes=image_bytes, mime_type="image/jpeg")

        operation = client.models.generate_videos(
             #텍스트 동영상 변환:
            # "dont_allow": 사람이나 얼굴이 포함되지 않습니다., 
            # "allow_adult": 성인은 포함하고 어린이는 포함하지 않는 동영상을 생성합니다. 
            # ,"allow_all": 성인 및 어린이가 포함된 동영상을 생성합니다.A
            #이미지-동영상 생성:
            # "dont_allow": 사람이나 얼굴이 포함되지 않습니다.
            # "allow_adult": 성인은 포함하고 어린이는 포함하지 않는 동영상을 생성합니다.
            # numberOfVideos: 요청된 동영상 출력(1 또는 2)입니다.
            # durationSeconds: 각 출력 동영상의 길이(초, 5~8)입니다.
            # enhance_prompt: 프롬프트 재작성기를 사용 설정 또는 중지합니다. 기본적으로 사용 설정됩니다.
            # model="veo-2.0-generate-001",
            model="veo-3.0-generate-preview",
            prompt=prompt,
            image=image,
            config=types.GenerateVideosConfig(
                aspect_ratio="16:9",  
                number_of_videos=1,
            ),
        )

        # Wait for videos to generate
        print("비디오 생성 중... (약 2-3분 소요)")
        check_count = 0
        while not operation.done:
            check_count += 1
            print(f"생성 진행 중... ({check_count}번째 확인)")
            time.sleep(20)
            operation = client.operations.get(operation)

        print("비디오 생성 완료! 파일 저장 중...")
        generated_videos = []
        if operation.response and operation.response.generated_videos:
            for n, video in enumerate(operation.response.generated_videos):
                if video and video.video:
                    # 고유한 파일명 생성 (타임스탬프 + 랜덤)
                    timestamp = int(time.time() * 1000)
                    unique_id = str(uuid.uuid4())[:8]
                    fname = f'video_{timestamp}_{unique_id}_{n}.mp4'
                    
                    # 전체 경로로 저장
                    full_path = os.path.join(output_dir, fname)
                    
                    client.files.download(file=video.video)
                    video.video.save(full_path)
                    generated_videos.append(full_path)
                    print(f"✅ 비디오 저장 완료: {fname}")
        
        return {
            "success": True,
            "videos": generated_videos,
            "message": f"{len(generated_videos)}개의 비디오가 성공적으로 생성되었습니다."
        }
                
    except Exception as e:
        print(f"비디오 생성 중 오류 발생: {e}")
        return {
            "success": False,
            "error": str(e),
            "message": "비디오 생성에 실패했습니다."
        }
                

def image_to_prompt(image_path: str, image_description: str , store_info : str):
    """
       이미지와 이미지에 대한 설명을 듣고 영상생성에 필요한 프롬프트와 비디오 위에 생성될 텍스트를 생성하는 함수
    """
    try:
        client = OpenAI()
        
        # 이미지를 base64로 인코딩
        with open(image_path, "rb") as f:
            image_bytes = f.read()
        
        # base64로 인코딩
        image_base64 = base64.b64encode(image_bytes).decode('utf-8')      
        
        SYSTEM_PROMPT = (
            "당신은 숏폼 광고 기획자/모션 디자이너다.\n"
            "입력(이미지+간단 설명)을 분석해 다음 형식의 JSON만 출력하라. 다른 문자는 금지.\n"
            "overlay_texts는 자막/텍스트 클립으로 바로 쓰일 짧은 한국어 문장\n"
            "영상 시간은 8초입니다. 8초동안 오버레이 텍스트가 모두 보여야 합니다.\n"
            
            "📋overlay_texts 기본 규칙 (3가지 적용)\n"
            "1. 오프닝 훅 (Hook) 전략\n"
            "여기가 [지역] 숨은 맛집!, 이 집 진짜 미쳤다..., [지역] 사람들만 아는 그 집!, 웨이팅 2시간 각오하세요! 중 하나 활용\n"
            "2. 정보 전달 우선순위\n"
            "1순위는 가게명, 대표메뉴, 핵심 가격이고 2순위는 위치, 특징, 맛 표현이며 3순위는 운영시간, 세트메뉴, 기타 정보\n"
            "3. 감정 어필 키워드\n"
            "맛 표현은 미쳤다, 대박, 폭발, 끝판왕이고 가성비는 딱, 저렴, 혜자, 가성비이며 추천은 저장, 필수, 인정, 레전드이고 긴급성은 서둘러, 빨리, 놓치지마 활용\n"
            
            "요구사항:\n"
            "- overlay_texts는 자막/텍스트 클립으로 바로 쓰일 짧은 한국어 문장 (7~18자 권장)\n"
            "- 과장 금지, 이미지 근거 디테일만 사용\n"
            "- video_prompt는 이전 포맷의 3파트(비주얼/오디오/감정) 요지를 포함하되 자유 형식\n"
            "- 각 항목 1~3문장, 불필요한 수식어/중복 금지\n"
            "- 촬영 동작(패닝/틸트/줌/슬로모션)은 1~2개만\n"
            "- 이미지는 가게 외관/가게 내부/요리하는 장면/요리로 나뉩니다.\n"
            "- 가게 외관: 왼쪽에서 오른쪽으로 화면을 이동하며 외관을 흩어 보여줄 것\n"
            "- 가게 내부: 왼쪽에서 오른쪽으로 화면을 이동하며 내부를 흩어 보여줄 것\n"
            "- 요리하는 장면: 생동감 있게 적절한 화면 움직임을 주고, 장면에 맞는 효과와 움직임을 과하지 않게 더할 것\n"
            "- 요리: 요리 종류에 맞게 숟가락 또는 젓가락으로 음식을 드는 모습을 적용할 것"
            
            "{\n"
            "  \"video_prompt\": string,   // VEO에 넣을 상세 프롬프트 (한국어)\n"
            "  \"overlay_texts\": [string, ...]  // 1~3개의 아주 짧고 강렬한 문구 (한국어)\n"
            "}\n"
            
        ) 
        
        FEWSHOT_1_INPUT = (
            "이미지 설명: 빨간 한글 간판 '박사반점', 콘크리트·벽돌 외벽, 전면 유리, "
            "창문 레터링과 ADT 보안 스티커, 가로수 반사"
        )

        FEWSHOT_1_OUTPUT = (
            "## 비주얼 소스\n"
            "메인 샷: 붉은 간판 글씨를 정면 클로즈업하여 브랜드명을 선명하게 강조한다.\n"
            "디테일 샷: 창문 레터링과 ADT 스티커를 교차 클로즈업해 신뢰감과 아이덴티티를 부각한다.\n"
            "오버뷰 샷: 전면 유리와 가로수 반사가 함께 보이도록 좌→우 패닝으로 외관 전체를 보여준다.\n"
            "조명·카메라: 자연광 위주. 간판 구간에서 짧게 줌 인으로 포인트를 준다.\n\n"
            "## 오디오 소스\n"
            "SFX: 문 열림 벨 소리, 바람에 흔들리는 나뭇잎 소리.\n"
            "음악: 경쾌한 브런치/재즈 또는 라이트 힙합, 밝은 템포.\n\n"
            "## 감정·컨셉 소스\n"
            "키워드: 동네 맛집, 깔끔함, 신뢰감, 방문 욕구.\n"
            "감정: '한번 가보고 싶다'는 호감과 기대를 유도한다."
        )
        
        # --- 예시 2: 주방 불쇼(셰프)
        FEWSHOT_2_INPUT = (
            "이미지 설명: 셰프 모자와 흰 제복, 메탈 주방, 웍에서 큰 불꽃이 치솟는 순간"
        )

        FEWSHOT_2_OUTPUT = (
            "## 비주얼 소스\n"
            "메인 샷: 웍에서 불꽃이 솟는 순간을 정면에서 슬로모션으로 잡아 임팩트를 준다.\n"
            "디테일 샷: 불 사이로 윤기 흐르는 재료, 셰프의 손동작과 집중된 표정을 클로즈업한다.\n"
            "오버뷰 샷: 메탈 질감의 넓은 주방 전경으로 전문성을 보여준다.\n"
            "조명·카메라: 불빛을 키라이트처럼 활용하고 주변 대비를 낮춰 불꽃을 강조한다.\n\n"
            "## 오디오 소스\n"
            "SFX: '후욱' 불꽃 치솟는 소리, '지글지글' 볶음 소리, 웍의 금속성 타격음.\n"
            "음악: 빠른 드럼/일렉트로닉 기반으로 박진감 강화.\n\n"
            "## 감정·컨셉 소스\n"
            "키워드: 불꽃 퍼포먼스, 장인정신, 강렬함, 기대감.\n"
            "감정: '프로다', '꼭 먹어봐야겠다'는 신뢰와 설렘을 전달한다."
        )
        
        # ========= 3) 실제 요청(이미지 + 설명) =========
        USER_MSG_TEXT = (
            f"사용자는 이 이미지를 '{image_description}' 라고 설명했습니다. "
            "가게에 대한 정보는 '{store_info}'입니다."
            "이 이미지를 바탕으로 영상 생성에 필요한 프롬프트를 위 포맷 그대로 작성해주세요."
        )

        messages = [
            # 시스템
            {
                "role": "system",
                "content": [{"type": "input_text", "text": SYSTEM_PROMPT}],
            },
            # Few-shot 1
            {
                "role": "user",
                "content": [{"type": "input_text", "text": FEWSHOT_1_INPUT}],
            },
            {
                "role": "assistant",
                "content": [{"type": "output_text", "text": FEWSHOT_1_OUTPUT}],
            },
            # Few-shot 2
            {
                "role": "user",
                "content": [{"type": "input_text", "text": FEWSHOT_2_INPUT}],
            },
            {
                "role": "assistant",
                "content": [{"type": "output_text", "text": FEWSHOT_2_OUTPUT}],
            },
            # 실제 입력(텍스트 + 이미지)
            {
                "role": "user",
                "content": [
                    {"type": "input_text", "text": USER_MSG_TEXT},
                    {
                        "type": "input_image",
                        "image_url": f"data:image/jpeg;base64,{image_base64}",
                        "detail": "high",
                    },
                ],
            },
        ]
                
        response = client.responses.create(
            model="gpt-4.1",
            input=messages,
        )
        
        import json as _json
        raw = response.output_text
        video_prompt = None
        overlay_texts = None
        try:
            obj = _json.loads(raw)
            video_prompt = obj.get("video_prompt")
            overlay_texts = obj.get("overlay_texts") or []
            if not isinstance(overlay_texts, list):
                overlay_texts = []
        except Exception:
            # JSON 파싱 실패 시, 전체 텍스트를 프롬프트로 두고 overlay_texts는 비워둠
            video_prompt = raw
            overlay_texts = []

        return {
            "success": True,
            "prompt": video_prompt,
            "overlays": overlay_texts,
            "message": "이미지 분석 완료"
        }
        
    except Exception as e:
        print(f"이미지 분석 중 오류 발생: {e}")
        return {
            "success": False,
            "error": str(e),
            "message": "이미지 분석에 실패했습니다."
        }


def merge_videos_sequentially(video_paths: List[str], output_filename: str = "merged_video.mp4"):
    
    """
    여러 비디오 파일을 순차적으로 합치는 함수
    
    Args:
        video_paths: 합칠 비디오 파일 경로들의 리스트
        output_filename: 출력 파일명
    
    Returns:
        dict: 성공 여부와 결과 정보
    """
    try:
        if not video_paths:
            return {
                "success": False,
                "error": "비디오 파일 경로가 없습니다.",
                "message": "합칠 비디오가 없습니다."
            }
        
        # 모든 비디오 파일이 존재하는지 확인하고 절대 경로로 변환
        absolute_video_paths = []
        for video_path in video_paths:
            # 상대 경로를 절대 경로로 변환
            if not os.path.isabs(video_path):
                absolute_path = os.path.abspath(video_path)
            else:
                absolute_path = video_path
            
            if not os.path.exists(absolute_path):
                return {
                    "success": False,
                    "error": f"비디오 파일을 찾을 수 없습니다: {absolute_path}",
                    "message": "일부 비디오 파일이 존재하지 않습니다."
                }
            
            absolute_video_paths.append(absolute_path)
        
        # 임시 파일에 비디오 경로 목록 작성
        with tempfile.NamedTemporaryFile(mode='w', suffix='.txt', delete=False) as f:
            for video_path in absolute_video_paths:
                f.write(f"file '{video_path}'\n")
            file_list_path = f.name
        
        try:
            # FFmpeg를 사용하여 비디오 합치기
            cmd = [
                'ffmpeg',
                '-f', 'concat',
                '-safe', '0',
                '-i', file_list_path,
                '-c', 'copy',  # 재인코딩 없이 복사 (빠름)
                '-y',  # 기존 파일 덮어쓰기
                output_filename
            ]
            
            print(f"비디오 합치기 시작: {len(video_paths)}개 파일")
            print(f"명령어: {' '.join(cmd)}")
            
            result = subprocess.run(cmd, capture_output=True, text=True, check=True)
            
            print("비디오 합치기 완료!")
            
            # 임시 파일 삭제
            os.unlink(file_list_path)
            
            return {
                "success": True,
                "merged_video": output_filename,
                "message": f"{len(video_paths)}개의 비디오가 성공적으로 합쳐졌습니다.",
                "output_path": os.path.abspath(output_filename)
            }
            
        except subprocess.CalledProcessError as e:
            print(f"FFmpeg 실행 오류: {e}")
            print(f"stdout: {e.stdout}")
            print(f"stderr: {e.stderr}")
            
            # 임시 파일 삭제
            try:
                os.unlink(file_list_path)
            except:
                pass
            
            return {
                "success": False,
                "error": f"FFmpeg 실행 실패: {e.stderr}",
                "message": "비디오 합치기에 실패했습니다."
            }
            
    except Exception as e:
        print(f"비디오 합치기 중 오류 발생: {e}")
        return {
            "success": False,
            "error": str(e),
            "message": "비디오 합치기에 실패했습니다."
        }
def burn_text_overlay_with_tts(video_in: str, overlay_lines, voice_gender: str, font_path="BMDOHYEON_ttf.ttf", out=None):
    """
        영상에 텍스트를 오버레이하고 TTS 음성을 합성하는 함수
    """
    from moviepy.audio.io.AudioFileClip import AudioFileClip
    
    # 1. TTS 파일 생성
    print("TTS 파일 생성 중...")
    # overlay_lines를 하나의 문자열로 합치기
    tts_script = " ".join(overlay_lines)
    tts_file_path = generate_tts_file(tts_script, voice_gender)
    print(f"TTS 파일 생성 완료: {tts_file_path}")
    
    # 2. 비디오 로드
    video = VideoFileClip(video_in)

    # 3. TTS 오디오 로드
    tts_audio = AudioFileClip(tts_file_path)
    
    # 4. 텍스트 오버레이 생성
    font_size = max(24, int(video.h * 0.08))
    clips = []
    # 각 라인당 2초씩
    # duration_per_line = 8 / len(overlay_lines)
    # 각 라인당 TTS 길이에 맞춰 조정
    duration_per_line = tts_audio.duration / len(overlay_lines)
    for i, line in enumerate(overlay_lines):
        start_time = i * duration_per_line

        kwargs = dict(
            text=line,
            font_size=font_size,       # MoviePy 2.x ; 1.x면 fontsize
            color="white",
            method="caption",
            size=(int(video.w * 0.9), None),
            bg_color="black",
        )
        if font_path and os.path.exists(font_path):
            kwargs["font"] = font_path

        txt = (
            TextClip(**kwargs)
            .with_opacity(0.85)   
            .with_position(("center", "center"))
            .with_start(start_time)
            .with_duration(duration_per_line)
        )
        clips.append(txt)

    # 5. 비디오와 텍스트 합성
    import uuid as _uuid, time as _time
    
    unique = f"{int(_time.time()*1000)}_{_uuid.uuid4().hex[:8]}"
    
    # 비디오에 텍스트 오버레이 추가
    video_with_text = CompositeVideoClip([video, *clips])
    
    # TTS 오디오를 비디오 길이에 맞춰 조정
    if tts_audio.duration > video.duration:
        tts_audio = tts_audio.subclipped(0, video.duration)
    elif tts_audio.duration < video.duration:
        # TTS가 짧으면 비디오를 TTS 길이에 맞춤
        video_with_text = video_with_text.subclipped(0, tts_audio.duration)
    
    # 6. 최종 합성 (텍스트 + TTS 음성)
    final = video_with_text.with_audio(tts_audio)
    
    # 7. 출력
    base, _ = os.path.splitext(video_in)
    out = out or f"{base}_{unique}_txt_tts.mp4"

    final.write_videofile(out, codec="libx264", audio_codec="aac", fps=video.fps or 30)
    output_path = os.path.abspath(out)
    
    # 8. 임시 TTS 파일 정리
    try:
        os.remove(tts_file_path)
        print(f"임시 TTS 파일 정리 완료: {tts_file_path}")
    except:
        pass
    
    return {
        "success": True,
        "message": "텍스트 번인 + TTS 합성 완료",
        "output_path": output_path,
    }

def generate_tts_file(
    tts_script : str,
    voice_gender : str
):
    """ 
        생성된 대본을 OPENAI TTS를 사용해서 TTS를 만들고
        TTS를 MoviePy를 사용해 믹스 후 반환
    """
    
    from pathlib import Path
    from openai import OpenAI
    
    voice = "echo" if voice_gender=="man" else "nova"

    client = OpenAI()
    unique_id = str(uuid.uuid4())[:8]
    
    # 영구 저장할 경로 설정
    output_dir = "generated_videos"
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
    
    speech_file_path = os.path.join(output_dir, f"speech_{unique_id}.mp3")
    
    with client.audio.speech.with_streaming_response.create(
        model="gpt-4o-mini-tts",
        voice=voice,
        input=tts_script,  # ✅ 전달받은 스크립트 사용
        # instructions="따뜻하고 또렷한 톤으로 읽어 주세요."
    ) as resp:
        resp.stream_to_file(speech_file_path)

    return speech_file_path
        
def generate_store_intro(
    descriptions : str ,
    store_info : str
):
    
    client = OpenAI()
    SYSTEM_PROMPT = (
        "당신은 SNS 홍보 전문 카피라이터다.\n"
        "입력(간단 설명 + 일부 데이터)을 분석해 음식점/상품에 대한 홍보글을 작성하라.\n"
        "\n"
        "조건:\n"
        "- 인스타그램/블로그에 올려도 자연스러운 홍보글로 작성\n"
        "- 가격은 반드시 강조 (저렴하면 가성비 표현, 비싸다면 가치 강조)\n"
        "- 맛이나 경험은 구어체로, '대박', '정말 맛있어요', '완전 만족' 같은 생생한 표현 활용\n"
        "- 글은 2~4 문단, 문단마다 1~3문장으로 구성\n"
        "- 각 문단마다 이모지 1~2개 사용 가능\n"
        "- 사용자가 입력한 장점이 있으면 반드시 포함해 사람들에게 어필\n"
        "- 마지막에는 해시태그 + 주소/위치 정보를 붙일 것\n"
        "- 총 글자 수: 300~500자 내외"
        
        "\n"
        "출력 형식:\n"
        "- 순수한 홍보글 텍스트만 출력 (JSON, 따옴표, 설명 문구, 불필요한 기호 금지)\n"
        "- 다른 불필요한 말은 절대 포함하지 말 것\n"
    )
    
    FEWSHOT_EXAMPLES = [
    {
        "INPUT": {
            "메뉴": "연탄생선구이백반",
            "가격": "12,000원 (제육 추가 8,000원)",
            "특징": "반찬 8가지, 청국장, 오징어국, 누룽지, 보리비빔밥 무제한",
            "조건": "점심 한정 판매"
        },
        "OUTPUT": """12000원에 이게 다 무제한이라고?😲
        여긴 생선구이 하나만 시켜도 반찬 8가지에 청국장, 오징어국, 누룽지, 보리비빔밥까지 전부 무제한이에요!
        여기에 8000원만 추가하면 제육볶음이 한가득 나와서 완전 대박😋

        보통 저렴하면 맛이 별로라서 안 가게 되는데, 다가생구이는 맛까지 보장이라 진짜 말도 안되는 곳이에요👍
        점심에만 파는 메뉴니까 꼭 점심에 가셔야 해요!

        📍 연탄생선구이백반 12,000원 / 제육볶음 8,000원
        #다가생구이서울본점 #마포맛집
        서울 마포구 새창로 4길 16-10 1층"""
    },
    {
        "INPUT": {
            "메뉴": "수제 치즈 돈까스",
            "가격": "9,500원",
            "특징": "치즈가 두툼하고 늘어남, 양이 푸짐함, 점심시간 대기 많음"
            },
        "OUTPUT": """치즈가 이렇게 늘어난다고?🧀✨
        9500원에 이 정도 퀄리티라니 가성비 최고!
        돈까스 한입 베어 물면 치즈가 줄줄 늘어나고, 양까지 푸짐해서 배 터질 뻔 했어요😋

        점심시간엔 손님이 너무 많아서 대기 필수지만 기다린 보람 200%!
        치즈덕후라면 무조건 가봐야 하는 곳이에요👍

        📍 수제 치즈 돈까스 9,500원
        #치즈돈까스맛집 #홍대맛집
        서울 마포구 양화로 12길 22"""
            },
            {
                "INPUT": {
                    "가게명": "복산돈까스",
                    "위치": "중구 반구동 300-1 번지",
                    "메뉴": {
                        "등심돈까스": "9,000원",
                        "치즈돈까스": "10,000원"
                    },
                    "영업시간": "월~금 10:00 ~ 18:00"
                },
                "OUTPUT": """점심 뭐 먹을지 고민이라면 여기 어떠세요?😋
        중구 반구동에 있는 복산돈까스는 등심돈까스가 단돈 9000원!
        치즈돈까스도 10000원에 치즈가 줄줄 늘어나서 진짜 대박이에요🧀✨

        가격은 저렴한데 맛과 양은 절대 저렴하지 않은 곳👍
        영업시간이 월~금 오전 10시부터 오후 6시까지만이라 주말엔 못 먹어요ㅠ 꼭 평일에 방문하세요!

        📍 복산돈까스 / 등심돈까스 9,000원 / 치즈돈까스 10,000원
        #복산돈까스 #중구맛집
        중구 반구동 300-1"""
            }
        ]
    
    user_descriptions = (
        f"""
        아래는 음식점에 대한 사용자 설명입니다.
        이 설명을 기반으로 음식점 홍보글을 작성하세요.
        설명이 부족하거나 누락된 부분이 있더라도 자연스럽게 보완해서 작성하세요.
        
        [가게 설명]
        {descriptions}
        
        [이미지 설명]
        {store_info}
        
        """
    )
    
    # 메시지 구성 (시스템 + few-shot + 최종 사용자 입력)
    messages = [
        {
            "role": "system",
            "content": [{"type": "input_text", "text": SYSTEM_PROMPT}],
        }
    ]
    # few-shot 예시 추가 (입력/출력 쌍을 번갈아 추가)
    for ex in FEWSHOT_EXAMPLES:
        example_input_text = str(ex.get("INPUT", ""))
        example_output_text = str(ex.get("OUTPUT", ""))
        if example_input_text:
            messages.append({
                "role": "user",
                "content": [{"type": "input_text", "text": example_input_text}],
            })
        if example_output_text:
            messages.append({
                "role": "assistant",
                "content": [{"type": "output_text", "text": example_output_text}],
            })

    # 실제 사용자 설명 입력
    messages.append({
        "role": "user",
        "content": [{"type": "input_text", "text": user_descriptions.format(description=descriptions)}],
    })

    # 호출 및 결과 반환
    try:
        response = client.responses.create(
            model="gpt-4.1",
            input=messages,
        )
        generated_text = (response.output_text or "").strip()
        return {
            "success": True,
            "intro": generated_text,
            "message": "홍보글 생성 완료",
        }
    except Exception as e:
        return {
            "success": False,
            "error": str(e),
            "message": "홍보글 생성에 실패했습니다.",
        }
    
