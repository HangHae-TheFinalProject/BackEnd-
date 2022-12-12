![main](https://user-images.githubusercontent.com/112993031/206946851-50dbdda6-2219-4e47-bcad-67d188ad3007.png)

# 라이어 게임 : 비밀의 문

## ✋ 프로젝트 소개
#### 예능으로만 보던 그 게임을 실시간으로 즐기는 온라인 게임입니다.

### 🔍 주요 기능
- `화상` 실시간으로 상대방의 표정을 관찰하면서 라이어를 찾아내세요.
- `채팅` 채팅으로 참가자들과 대화를 나누며 라이어를 찾아내세요.
- `투표` 참가자들의 투표를 통해 라이어를 찾아내세요.

<details>
<summary>주요 기술</summary>

  #### OpenVidu
  - MCU 방식은 WebRTC를 사용하는 이유인 실시간성이 저해되고  
  비디오, 오디오와 같은 미디어 자원을 사용하는 데에 있어 비용이 많이 든다는 단점이 있습니다.  
  Mesh 방식은 클라이언트끼리 직접적으로 연결되어 클라이언트의 과부하가 급격하게 증가합니다.  
  따라서, 다대다 WebRTC 연결방식 중 클라이언트의 부하가 적은 SFU 방식 선택하였고,  
  SFU연결에 필요한 미디어 서버 구축에 필요한 리소스를 절약해주는 open-vidu 라이브러리 이용하였습니다.
  #### SockJS/StompJS
  - Websocket 으로 헤더 전송이 어려워 보안된 인증처리 구현이 어렵다. 반면 Stomp는 메세지 헤더를 통해  
  인증 처리를 구현하기 용이하며, pub/sub 구조의 Websocket기반 프로토콜로 메세지 발행 시 엔드포인트를  
  별도로 분리하여 관리하기 용이하여 선택하였다.
  #### CI/CD
  - 자동 배포 방식 중 ‘Amazon AWS S3/Git Action’과 ‘Amazon AWS Amplify’ 두 가지 방법을 고려하였고,  
  Open-vidu의 경우 https 프로토콜 사용이 필수 적인데 ‘Amazon S3/Git Action’ 방식은 http 프로토콜 배포를  
  지원하지 않아 ‘Amazon AWS Amplify’ 배포 방식을 사용하게 되었다.

</details>

### 🎮 인게임 대표 이미지
![ingame](https://user-images.githubusercontent.com/112993031/206951497-8d6f778b-335b-4889-8214-addb59ec86a0.png)

### 🛠️ 트러블슈팅
<details>
<summary>프론트엔드 트러블슈팅</summary>

  #### OpenVidu
  - ⛔️ 문제  
  다른 브라우저에서 카메라를 사용 중인 경우 Open-vidu 서버에 접속에 실패하는데, 이 때 나타나는  
  오류코드가 Open-vidu logger. 즉, 라이브러리 내에서 발생한 코드여서 분기점을 잡기가 어려웠습니다.
  - ✅ 해결  
  `navigator.mediaDevices.getUserMedia` 함수를 통해 내가 원하는 곳에 분기점을 추가로 잡을 수 있을  
  것으로 판단했고 open-vidu 접속보다 보다 앞선 GameRoom useEffect에서 미리 분기점을 잡아 예외처리 했습니다.

</details>
<details>
<summary>백엔드 트러블슈팅</summary>

  #### QueryDSL
  - ⛔️ 문제  
  쿼리문 작성 후 실행 시, Update문과 같은 bulk연산을 자주 실행할 때 정상적으로 영속성 컨텍스트에  
  반영이 되지 않아 이전 데이터가 그대로 남아있는 상태로 보여지는 이슈가 잦았습니다..
  - ✅ 해결  
  이전에는 무조건 flush()와 같은 명령어를 무분별하게 사용하여 제대로 데이터가 반영이 되지 않았다면,  
  이번에는 api가 정상적으로 실행이 되고 메소드 내부 마지막 부분에 기입하여 한번에 반영될 수 있도록 수정했습니다.

</details>

전체 트러블 슈팅 <노션 링크> ▶ https://www.notion.so/Trouble-shooting-ea14209f725342b68610b5c0fa77728a

### 🗂️ 기술스택
#### 프론트엔드
<img src="https://img.shields.io/badge/HTML-E34F26?style=for-the-badge&logo=HTML5&logoColor=white"/> <img src="https://img.shields.io/badge/Sass-CC6699?style=for-the-badge&logo=Sass&logoColor=white"/> <img src="https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=JavaScript&logoColor=black"/> <img src="https://img.shields.io/badge/React-61DAFB?style=for-the-badge&logo=React&logoColor=black"/> <img src="https://img.shields.io/badge/Redux Toolkit-764ABC?style=for-the-badge&logo=Redux&logoColor=white"/> <br> <img src="https://img.shields.io/badge/Axios-5A29E4?style=for-the-badge&logo=Axios&logoColor=white"/> <img src="https://img.shields.io/badge/WebRTC-333333?style=for-the-badge&logo=WebRTC&logoColor=white"/> <img src="https://img.shields.io/badge/openVidu-06d362?style=for-the-badge&logo=oepnVidu&logoColor=white"/> <img src="https://img.shields.io/badge/sockjs-333333?style=for-the-badge&logo=sockjs&logoColor=white"/> <img src="https://img.shields.io/badge/stomp-333333?style=for-the-badge&logo=stomp&logoColor=white"/>
#### 백엔드
<img src="https://img.shields.io/badge/Spring-6DB33F?style=for-the-badge&logo=Spring&logoColor=white"/> <img src="https://img.shields.io/badge/Spring Boot-6DB33F?style=for-the-badge&logo=Spring Boot&logoColor=white"/> <img src="https://img.shields.io/badge/Spring Security-6DB33F?style=for-the-badge&logo=Spring Security&logoColor=white"/> <img src="https://img.shields.io/badge/JWT Token-333333?style=for-the-badge&logo=JWT Token&logoColor=white"/>  
<img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=Redis&logoColor=white"/> <img src="https://img.shields.io/badge/QueryDSL-0769AD?style=for-the-badge&logo=QueryDSL&logoColor=white"/> <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=MySQL&logoColor=white"/> <img src="https://img.shields.io/badge/Amazon RDS-527FFF?style=for-the-badge&logo=Amazon RDS&logoColor=white"/> <img src="https://img.shields.io/badge/Amazon S3-569A31?style=for-the-badge&logo=Amazon S3&logoColor=white"/>  
<img src="https://img.shields.io/badge/Amazon EC2-FF9900?style=for-the-badge&logo=Amazon EC2&logoColor=white"/> <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=Docker&logoColor=white"/> <img src="https://img.shields.io/badge/WebRTC-333333?style=for-the-badge&logo=WebRTC&logoColor=white"/> <img src="https://img.shields.io/badge/openVidu-06d362?style=for-the-badge&logo=oepnVidu&logoColor=white"/> <img src="https://img.shields.io/badge/sockjs-333333?style=for-the-badge&logo=sockjs&logoColor=white"/> <img src="https://img.shields.io/badge/stomp-333333?style=for-the-badge&logo=stomp&logoColor=white"/>

### 🏗️ 프로젝트 아키텍처
![서비스 아키텍쳐 #중간발표](https://user-images.githubusercontent.com/112993031/204065939-8d25f487-30cb-43d0-ab3a-1a663ccf8335.png)
서비스 아키텍쳐 설명 <노션 링크> ▶ https://www.notion.so/Service-Architecture-a0a8b52c030641d59b1ad31fce0893a0

### 📆 프로젝트 기간

> 2022.11.04 ~ 2022.12.16  
> 서비스 런칭 : 2022.12.08  

### 🙋🏻‍♀️🙋🏻 프로젝트 멤버 (이름을 클릭하시면 깃허브로 이동할 수 있습니다.)

|[김민주](https://github.com/roses16-dev) 🌞|[신혜정](https://github.com/cherrydding)|[정건희](https://github.com/keepfall)|[진세훈](https://github.com/JayEsEichi) 🌝|[조혜수](https://github.com/antcho1024)|박성빈|
|:---:|:---:|:---:|:---:|:---:|:---:|
|![Screenshot 2022-12-12 at 11 44](https://user-images.githubusercontent.com/112993031/206950033-8610fec1-2a85-46d8-9283-a18e110e32c2.png)|![Screenshot 2022-12-12 at 11 38-1](https://user-images.githubusercontent.com/112993031/206950056-e90ba3e7-d3e7-40f2-9cb4-ff7a0d77c939.png)|![Screenshot 2022-12-12 at 11 38-2](https://user-images.githubusercontent.com/112993031/206950052-99318217-626e-4f68-8815-e1f5e1f316e5.png)|![Screenshot 2022-12-12 at 11 38](https://user-images.githubusercontent.com/112993031/206950045-de2696fd-ccae-4431-8ac9-26b724727c56.png)|![Screenshot 2022-12-12 at 11 41](https://user-images.githubusercontent.com/112993031/206950040-b38b2b2d-5da1-4ad9-9e6a-f31658635e17.png)|![Screenshot 2022-12-12 at 11 38-1](https://user-images.githubusercontent.com/112993031/206950056-e90ba3e7-d3e7-40f2-9cb4-ff7a0d77c939.png)|
|FRONT-END|FRONT-END|FRONT-END|BECK-END|BECK-END|DESIGN|
