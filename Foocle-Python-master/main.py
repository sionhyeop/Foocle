import logging
import os
from contextlib import asynccontextmanager
import time
from typing import Optional, Dict, Any # asynccontextmanager 임포트
from fastapi import FastAPI, HTTPException, File, Request, UploadFile, Form, APIRouter, BackgroundTasks
from pydantic import BaseModel
from datetime import datetime
import tempfile
import shutil
from fastapi.responses import StreamingResponse, FileResponse
import io
from PIL import Image
# from database import database, connect_db, disconnect_db, create_tables_if_not_exists
from gemini import burn_text_overlay_with_tts, generate_store_intro, generate_video, image_to_prompt, merge_videos_sequentially

# lifespan 컨텍스트 매니저 정의
@asynccontextmanager
async def lifespan(app: FastAPI):
    # 앱 시작 시 실행될 코드
    print("FastAPI 앱 시작 중...")
    print("FastAPI 앱 시작 완료.")
    yield
    print("FastAPI 앱 종료 중...")
    print("FastAPI 앱 종료 완료.")

# FastAPI 앱 정의 시 lifespan 전달
app = FastAPI(
    title="matstar API",
    description="비디오 생성 및 데이터베이스 연동 API.",
    version="0.1.0",
    lifespan=lifespan # <--- 여기에서 lifespan 컨텍스트 매니저를 연결합니다.
)
# app.include_router(ai_router)

# 응답 모델 (Pydantic)
class VideoGenerateResponse(BaseModel):
    id: int
    prompt: str
    input_image_gcs_uri: str
    output_video_gcs_uri: str
    aspect_ratio: str
    created_at: datetime

    class Config:
        from_attributes = True

@app.get("/ai")
async def root():
    return {"message": "Welcome to the foocle API!"}

@app.get("/ai/health")
def health_check():
    return {"status": "ok"}


# @app.post("/ai/generate-video")
# async def generate_food_video(
#     prompt: str = Form(...),
#     input_image1: UploadFile = File(..., description="첫 번째 이미지"),
#     image1_description: str = Form(..., description="첫 번째 이미지 설명"),
#     input_image2: UploadFile = File(..., description="두 번째 이미지"),
#     image2_description: str = Form(..., description="두 번째 이미지 설명"),
#     input_image3: UploadFile = File(..., description="세 번째 이미지"),
#     image3_description: str = Form(..., description="세 번째 이미지 설명"),
#     input_image4: UploadFile = File(..., description="네 번째 이미지"),
#     image4_description: str = Form(..., description="네 번째 이미지 설명")
# ):
#     try:
#         # 4개의 이미지와 설명을 함께 저장
#         temp_image_paths = []
#         image_descriptions = []
#         input_images = [input_image1, input_image2, input_image3, input_image4]
#         descriptions = [image1_description, image2_description, image3_description, image4_description]
        
#         for i, (input_image, description) in enumerate(zip(input_images, descriptions)):
#             # 이미지를 임시 파일로 저장
#             with tempfile.NamedTemporaryFile(delete=False, suffix=".jpg") as temp_file:
#                 shutil.copyfileobj(input_image.file, temp_file)
#                 temp_image_paths.append(temp_file.name)
            
#             # AI로 이미지 분석
#             ai_analysis = image_to_prompt(temp_file.name, description)
#             if ai_analysis["success"]:
#                 ai_description = ai_analysis["description"]
#                 print(f"AI 분석 결과: {ai_description}")
#             else:
#                 ai_description = "AI 분석 실패"
#                 print(f"AI 분석 실패: {ai_analysis['message']}")
            
#             # 사용자 설명과 AI 분석을 결합
#             combined_description = f"사용자 설명: {description}\nAI 분석: {ai_description}"
#             image_descriptions.append(combined_description)
            
#             print(f"이미지 {i+1}: {description}")
#             print(f"파일 경로: {temp_file.name}")
#             print(f"결합된 설명: {combined_description}")
#             print("-" * 50)
        
#         # 모든 이미지 설명을 포함한 향상된 프롬프트 생성
#         enhanced_prompt = f"{prompt}\n\n이미지 설명들:\n"
#         for i, desc in enumerate(image_descriptions, 1):
#             enhanced_prompt += f"이미지 {i}: {desc}\n"
        
#         print(f"향상된 프롬프트: {enhanced_prompt}")
        
#         # 비디오 생성 (첫 번째 이미지를 메인으로 사용하거나, 모든 이미지를 처리하는 로직으로 수정 필요)
#         result = generate_video(enhanced_prompt, temp_image_paths[0])  # 임시로 첫 번째 이미지만 사용
        
#         if not result["success"]:
#             raise HTTPException(status_code=500, detail=result["message"])
        
#         # 임시 파일들 삭제
#         for temp_path in temp_image_paths:
#             try:
#                 os.unlink(temp_path)
#             except:
#                 pass
        
#         # 생성된 비디오 파일 경로
#         video_path = result["videos"][0] if result["videos"] else None
#         if not video_path:
#             raise HTTPException(status_code=500, detail="비디오 파일이 생성되지 않았습니다.")
        
#         # 비디오 파일을 바로 응답
#         return FileResponse(
#             path=video_path,
#             media_type="video/mp4",
#             filename=os.path.basename(video_path)
#         )
        
#     except Exception as e:
#         # 임시 파일들이 남아있다면 삭제
#         if 'temp_image_paths' in locals():   
#             for temp_path in temp_image_paths:
#                 try:
#                     os.unlink(temp_path)
#                 except:
#                     pass
        
#         raise HTTPException(status_code=500, detail=f"비디오 생성 실패: {str(e)}")
    
# @app.post("/ai/test")    
# async def test_text(
#     store_info: str = Form(..., description="가게 정보"),
#     input_image1: UploadFile = File(..., description="첫 번째 이미지"),
#     image1_description: str = Form(..., description="첫 번째 이미지 설명"),
# ):
#     """
#     gemini.py의 함수들을 테스트하는 엔드포인트
#     - image_to_prompt 함수 테스트
#     - generate_video 함수 테스트
#     """
#     try:
#         import tempfile
#         import shutil
        
#         # 1. 이미지를 임시 파일로 저장
#         with tempfile.NamedTemporaryFile(delete=False, suffix=".jpg") as temp_file:
#             shutil.copyfileobj(input_image1.file, temp_file)
#             temp_image_path = temp_file.name
        
#         print(f"📁 임시 이미지 저장: {temp_image_path}")
#         print(f"📝 이미지 설명: {image1_description}")
        
#         # 2. image_to_prompt 함수 테스트
#         print("🔄 image_to_prompt 함수 테스트 중...")
#         from gemini import image_to_prompt
        
#         prompt_result = image_to_prompt(temp_image_path, image1_description,store_info)
        
#         if not prompt_result["success"]:
#             # 임시 파일 삭제
#             try:
#                 os.unlink(temp_image_path)
#             except:
#                 pass
#             raise HTTPException(status_code=500, detail=f"이미지 분석 실패: {prompt_result['message']}")
        
#         generated_prompt = prompt_result["prompt"]
        
#         # generated_prompt = (
#         #     """ 비주얼 소스\n메인 샷: 토마토 페페로니 피자를 위에서 정면으로 촬영해 노릇한 도우와 신선한 토핑을 선명하게 부각한다.\n디테일 샷: 치즈와 바질 잎 위주로 클로즈업해 식감과 신선함을 강조한다.\n오버뷰 샷: 피자가 놓인 원목 테이블과 자연광, 그림자가 함께 보이도록 테이블 위를 부드럽게 줌 아웃한다.\n조명·카메라: 자연광을 활용해 토핑의 색감을 살리고, 부드러운 패닝으로 따뜻한 분위기를 연출한다.\n\n## 오디오 소스\nSFX: 바삭하게 피자 커팅하는 소리, 치즈가 늘어나는 소리.\n음악: 밝고 경쾌한 어쿠스틱 기타.\n\n## 감정·컨셉 소스\n키워드: 집에서 인기, 신선함, 따뜻함, 정성.\n감정: 먹어보고 싶어지는 마음, 소소한 행복과 만족"""
#         # )
#         print(f"✅ 프롬프트 생성 성공: {generated_prompt[:100]}...")
        
#         # 3. generate_video 함수 테스트 (선택사항)
#         test_video = True  # 비디오 생성 테스트를 원하면 True로 변경
        
#         if test_video:
#             print("🔄 generate_video 함수 테스트 중... (약 2-3분 소요)")
#             from gemini import generate_video
            
#             video_result = generate_video(generated_prompt, temp_image_path)
            
#             if not video_result["success"]:
#                 # 임시 파일 삭제
#                 try:
#                     os.unlink(temp_image_path)
#                 except:
#                     pass
#                 raise HTTPException(status_code=500, detail=f"비디오 생성 실패: {video_result['message']}")
            
#             print(f"✅ 비디오 생성 성공: {video_result['videos']}")
#         else:
#             print("⏭️ 비디오 생성 테스트는 건너뜀 (test_video = False)")
        
#         # 4. 임시 파일 삭제
#         try:
#             os.unlink(temp_image_path)
#         except:
#             pass
        
#         # 5. 결과 반환
#         return {
#             "success": True,
#             "message": "테스트 완료",
#             "image_analysis": {
#                 "success": prompt_result["success"],
#                 "overlays": prompt_result["overlays"],
#                 "message": prompt_result["message"],
#                 "prompt" : prompt_result["prompt"]
#             },
#             "video_generation": {
#                 "tested": test_video,
#                 "success": video_result["success" ] if test_video else None,
#                 "videos": video_result.get("videos", []) if test_video else None,
#                 "message": video_result.get("message", "") if test_video else "테스트하지 않음"
#             }
#         }
        
#     except Exception as e:
#         # 임시 파일이 남아있다면 삭제
#         if 'temp_image_path' in locals():   
#             try:
#                 os.unlink(temp_image_path)
#             except:
#                 pass
        
#         raise HTTPException(status_code=500, detail=f"테스트 실패: {str(e)}")
        

@app.post("/ai/generate-store-intro")
async def generate_store_intr_def(   
    store_info: str =Form(...),
    descriptions : str = Form(...)
):
        store_intro = generate_store_intro(descriptions=descriptions, store_info=store_info)
        if store_intro["success"]:
            return {
                "success" : store_intro["success"],
                "store_intro" : store_intro["intro"],
                "message" : store_intro["message"]
            }
        else:
        # 실패 시, HTTP 400 Bad Request 예외를 발생시킵니다.
            raise HTTPException(status_code=400, detail="상점 소개글 생성에 실패했습니다. 필요한 정보를 모두 제공했는지 확인하세요.")


@app.post("/ai/generate-merged-video-dynamic")
async def generate_merged_video_dynamic(
    store_info: str = Form(...),
    images: list[UploadFile] = File(..., description="이미지 파일들"),
    description: str = Form(..., description="이미지 설명들을 , 로 구분해서 입력"),
    tts_gender: str = Form(...)
):
    """
    동적 개수의 이미지를 받아서 각각 비디오로 생성하고 순차적으로 합치는 API
    """
    
    logger = logging.getLogger("uvicorn")
    start = time.time()
    
    descriptions = [d.strip() for d in description.split(",")]
    
    temp_image_paths = []
    generated_video_paths = []
    
    try:
        # 이미지와 설명 개수 검증
        if len(images) != len(descriptions):
            raise HTTPException(
                status_code=400, 
                detail=f"이미지 개수({len(images)})와 설명 개수({len(descriptions)})가 일치하지 않습니다."
            )
            
        # return {
        # "store_info": store_info,
        # "images": [img.filename for img in images],
        # "descriptions": descriptions,
        # "hi" : description_list[1]

        if len(images) == 0:
            raise HTTPException(status_code=400, detail="최소 1개의 이미지가 필요합니다.")
        
        if len(images) > 10:  # 최대 10개로 제한
            raise HTTPException(status_code=400, detail="최대 10개의 이미지만 처리 가능합니다.")
        
        print(f"🔄 {len(images)}개 이미지 처리 및 비디오 생성 시작...")
        logger.info("1단계 시작: 이미지 확인")
        # 각 이미지별로 비디오 생성
        for i, (input_image, description) in enumerate(zip(images, descriptions)):
            print(f"\n📸 이미지 {i+1}/{len(images)} 처리 중..." , flush=True)
            
            # 이미지를 임시 파일로 저장
            with tempfile.NamedTemporaryFile(delete=False, suffix=".jpg") as temp_file:
                shutil.copyfileobj(input_image.file, temp_file)
                temp_image_paths.append(temp_file.name)
            
            # AI로 이미지 분석하여 프롬프트 생성
            ai_analysis = image_to_prompt(temp_file.name, description, store_info)
            if ai_analysis["success"]:
                ai_video_prompt = ai_analysis["prompt"]
                print(f"✅ AI 분석 완료: {ai_video_prompt[:100]}...", flush=True)
            else:
                ai_video_prompt = description  # AI 분석 실패시 원본 설명 사용
                print(f"⚠️ AI 분석 실패, 원본 설명 사용: {description}")
                
            # ai_prompt = ""    
            
            # # 사용자 프롬프트와 AI 분석을 결합
            # combined_prompt = f"{prompt}\n\n이미지 {i+1} 상세 설명:\n{ai_prompt}"
            
            # 개별 비디오 생성
            logger.info(f"1단계 완료 ({time.time()-start:.2f}초 소요)")
            print(f"🎬 비디오 {i+1} 생성 중... (약 2-3분 소요)", flush=True)
            video_result = generate_video(ai_video_prompt, temp_file.name)
            
            if video_result["success"] and video_result["videos"]:
                # 생성된 각 비디오에 오버레이 적용 후 경로 수집
                for video_path in video_result["videos"]:
                    absolute_path = os.path.abspath(video_path) if not os.path.isabs(video_path) else video_path
                    overlay_texts = ai_analysis.get("overlays", [])

                    try:
                        overlay_res = burn_text_overlay_with_tts(
                            video_in=absolute_path,
                            overlay_lines=overlay_texts,
                            voice_gender=tts_gender
                        )
                        if isinstance(overlay_res, dict) and overlay_res.get("success"):
                            absolute_path = overlay_res.get("output_path", absolute_path)
                            logger.info(f"2단계 완료 ({time.time()-start:.2f}초 소요)")
                        else:
                            print(f"⚠️ 오버레이 적용 실패, 원본 사용: {overlay_res}")
                    except Exception as _ovr_e:
                        print(f"⚠️ 오버레이 처리 중 예외, 원본 사용: {_ovr_e}")

                    generated_video_paths.append(absolute_path)
                print(f"✅ 비디오 {i+1} 생성(+오버레이) 완료: {generated_video_paths[-len(video_result['videos']):]}")
            else:
                print(f"❌ 비디오 {i+1} 생성 실패: {video_result.get('message', '알 수 없는 오류')}")
                raise HTTPException(status_code=500, detail=f"비디오 {i+1} 생성 실패: {video_result.get('message', '알 수 없는 오류')}")
        
        # 생성된 비디오들을 하나로 합치기
        if len(generated_video_paths) > 1:
            print(f"\n🔗 {len(generated_video_paths)}개의 비디오 합치기 시작...")
            merged_result = merge_videos_sequentially(generated_video_paths, "merged_shorts.mp4")
            
            if merged_result["success"]:
                print(f"✅ 비디오 합치기 완료: {merged_result['merged_video']}")
                final_video_path = merged_result["merged_video"]
                logger.info(f"3단계 완료 ({time.time()-start:.2f}초 소요)")
            else:
                print(f"❌ 비디오 합치기 실패: {merged_result.get('message', '알 수 없는 오류')}")
                raise HTTPException(status_code=500, detail=f"비디오 합치기 실패: {merged_result.get('message', '알 수 없는 오류')}")
        else:
            # 비디오가 하나만 있는 경우
            final_video_path = generated_video_paths[0]
            print(f"✅ 단일 비디오 사용: {final_video_path}")
        
        # 임시 이미지 파일들 삭제
        for temp_path in temp_image_paths:
            try:
                os.unlink(temp_path)
            except:
                pass
        
        # 개별 비디오 파일들 삭제 (합쳐진 후)
        for video_path in generated_video_paths:
            try:
                if video_path != final_video_path:  # 최종 합쳐진 파일은 유지
                    os.unlink(video_path)
            except:
                pass
        
        
        # 합친 영상에 TTS+자막 번인 적용
        # try:
        #     tts_out = os.path.splitext(final_video_path)[0] + "_tts.mp4"
        #     tts_result = videos_tts(video_in=final_video_path, video_out=tts_out, subs=None)
        #     if tts_result.get("success"):
        #         final_video_path = tts_result["output_path"]
        #     else:
        #         print(f"⚠️ TTS 합성 실패: {tts_result.get('message')} - 원본 병합 영상으로 반환합니다.")
        # except Exception as _e:
        #     print(f"⚠️ TTS 처리 중 예외: {_e}")

        # 최종 비디오 파일을 응답으로 반환
        return FileResponse(
            path=final_video_path,
            media_type="video/mp4",
            filename=os.path.basename(final_video_path),
        )
        
    except Exception as e:
        # 오류 발생시 모든 임시 파일 정리
        print(f"❌ 오류 발생: {str(e)}")
        
        # 임시 이미지 파일들 삭제
        for temp_path in temp_image_paths:
            try:
                os.unlink(temp_path)
            except:
                pass
        
        # 생성된 비디오 파일들 삭제
        for video_path in generated_video_paths:
            try:
                os.unlink(video_path)
            except:
                pass
        
        raise HTTPException(status_code=500, detail=f"합쳐진 비디오 생성 실패: {str(e)}")


# @app.post("/ai/generate-merged-video-json")
# async def generate_merged_video_json(
#     request: Request
# ):
#     """
#     JSON 형태로 이미지 URL과 설명을 받아서 비디오를 생성하고 합치는 API
#     """
#     try:
#         # JSON 데이터 파싱
#         data = await request.json()
#         prompt = data.get("prompt", "")
#         images_data = data.get("images", [])
        
#         if not images_data:
#             raise HTTPException(status_code=400, detail="이미지 데이터가 없습니다.")
        
#         if len(images_data) > 10:  # 최대 10개로 제한
#             raise HTTPException(status_code=400, detail="최대 10개의 이미지만 처리 가능합니다.")
        
#         temp_image_paths = []
#         generated_video_paths = []
        
#         print(f"🔄 {len(images_data)}개 이미지 처리 및 비디오 생성 시작...")
        
#         # 각 이미지별로 비디오 생성
#         for i, image_data in enumerate(images_data):
#             image_url = image_data.get("url")
#             description = image_data.get("description", "")
            
#             if not image_url:
#                 raise HTTPException(status_code=400, detail=f"이미지 {i+1}의 URL이 없습니다.")
            
#             print(f"\n📸 이미지 {i+1}/{len(images_data)} 처리 중...")
#             print(f"URL: {image_url}")
#             print(f"설명: {description}")
            
#             # 이미지 다운로드
#             import requests
#             try:
#                 response = requests.get(image_url, timeout=30)
#                 response.raise_for_status()
                
#                 # 임시 파일로 저장
#                 with tempfile.NamedTemporaryFile(delete=False, suffix=".jpg") as temp_file:
#                     temp_file.write(response.content)
#                     temp_image_paths.append(temp_file.name)
                
#             except Exception as e:
#                 raise HTTPException(status_code=400, detail=f"이미지 {i+1} 다운로드 실패: {str(e)}")
            
#             # AI로 이미지 분석하여 프롬프트 생성
#             ai_analysis = image_to_prompt(temp_file.name, description)
#             if ai_analysis["success"]:
#                 ai_prompt = ai_analysis["prompt"]
#                 print(f"✅ AI 분석 완료: {ai_prompt[:100]}...")
#             else:
#                 ai_prompt = description  # AI 분석 실패시 원본 설명 사용
#                 print(f"⚠️ AI 분석 실패, 원본 설명 사용: {description}")
            
#             # 사용자 프롬프트와 AI 분석을 결합
#             combined_prompt = f"{prompt}\n\n이미지 {i+1} 상세 설명:\n{ai_prompt}"
            
#             # 개별 비디오 생성
#             print(f"🎬 비디오 {i+1} 생성 중... (약 2-3분 소요)")
#             video_result = generate_video(combined_prompt, temp_file.name)
            
#             if video_result["success"] and video_result["videos"]:
#                 # 생성된 비디오 경로들을 절대 경로로 변환하여 추가
#                 for video_path in video_result["videos"]:
#                     if not os.path.isabs(video_path):
#                         absolute_path = os.path.abspath(video_path)
#                     else:
#                         absolute_path = video_path
#                     generated_video_paths.append(absolute_path)
#                 print(f"✅ 비디오 {i+1} 생성 완료: {video_result['videos']}")
#             else:
#                 print(f"❌ 비디오 {i+1} 생성 실패: {video_result.get('message', '알 수 없는 오류')}")
#                 raise HTTPException(status_code=500, detail=f"비디오 {i+1} 생성 실패: {video_result.get('message', '알 수 없는 오류')}")
        
#         # 생성된 비디오들을 하나로 합치기
#         if len(generated_video_paths) > 1:
#             print(f"\n🔗 {len(generated_video_paths)}개의 비디오 합치기 시작...")
#             merged_result = merge_videos_sequentially(generated_video_paths, "merged_shorts.mp4")
            
#             if merged_result["success"]:
#                 print(f"✅ 비디오 합치기 완료: {merged_result['merged_video']}")
#                 final_video_path = merged_result["merged_video"]
#             else:
#                 print(f"❌ 비디오 합치기 실패: {merged_result.get('message', '알 수 없는 오류')}")
#                 raise HTTPException(status_code=500, detail=f"비디오 합치기 실패: {merged_result.get('message', '알 수 없는 오류')}")
#         else:
#             # 비디오가 하나만 있는 경우
#             final_video_path = generated_video_paths[0]
#             print(f"✅ 단일 비디오 사용: {final_video_path}")
        
#         # 임시 이미지 파일들 삭제
#         for temp_path in temp_image_paths:
#             try:
#                 os.unlink(temp_path)
#             except:
#                 pass
        
#         # 개별 비디오 파일들 삭제 (합쳐진 후)
#         for video_path in generated_video_paths:
#             try:
#                 if video_path != final_video_path:  # 최종 합쳐진 파일은 유지
#                     os.unlink(video_path)
#             except:
#                 pass
        
#         # 합친 영상에 TTS+자막 번인 적용
#         try:
#             tts_out = os.path.splitext(final_video_path)[0] + "_tts.mp4"
#             tts_result = videos_tts(video_in=final_video_path, video_out=tts_out, subs=None)
#             if tts_result.get("success"):
#                 final_video_path = tts_result["output_path"]
#             else:
#                 print(f"⚠️ TTS 합성 실패: {tts_result.get('message')} - 원본 병합 영상으로 반환합니다.")
#         except Exception as _e:
#             print(f"⚠️ TTS 처리 중 예외: {_e}")

#         # 최종 비디오 파일을 응답으로 반환
#         return FileResponse(
#             path=final_video_path,
#             media_type="video/mp4",
#             filename=os.path.basename(final_video_path)
#         )
        
#     except Exception as e:
#         # 오류 발생시 모든 임시 파일 정리
#         print(f"❌ 오류 발생: {str(e)}")
        
#         if 'temp_image_paths' in locals():
#             for temp_path in temp_image_paths:
#                 try:
#                     os.unlink(temp_path)
#                 except:
#                     pass
        
#         if 'generated_video_paths' in locals():
#             for video_path in generated_video_paths:
#                 try:
#                     os.unlink(video_path)
#                 except:
#                     pass
        
#         raise HTTPException(status_code=500, detail=f"합쳐진 비디오 생성 실패: {str(e)}")


