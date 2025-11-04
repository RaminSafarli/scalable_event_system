// import http from "k6/http";
// import { check } from "k6";

// export const options = {
//   // define thresholds
//   thresholds: {
//     http_req_failed: [{ threshold: "rate<0.01", abortOnFail: true }], // http errors should be less than 1%
//     http_req_duration: ["p(99)<1000"], // 99% of requests should be below 1s
//   },
//   scenarios: {
//     // define scenarios
//     breaking: {
//       executor: "ramping-vus",
//       stages: [
//         { duration: "10s", target: 20 },
//         { duration: "50s", target: 20 },
//         { duration: "50s", target: 40 },
//         { duration: "50s", target: 60 },
//         { duration: "50s", target: 80 },
//         { duration: "50s", target: 100 },
//         { duration: "50s", target: 120 },
//         { duration: "50s", target: 140 },
//         //....
//       ],
//     },
//   },
// };

// export default function () {
//   const url = "http://localhost:8081/ticket";
//   const payload = JSON.stringify({
//     user_id: 1,
//     event_id: 1,
//   });
//   const params = {
//     headers: {
//       "Content-Type": "application/json",
//     },
//   };

//   const res = http.post(url, payload, params);

//   check(res, {
//     "response code was 202": (res) => res.status == 202,
//   });
// }

import http from "k6/http";
import { sleep } from "k6";

export default function () {
  const payload = JSON.stringify({ user_id: 123, event_id: 456 });
  const params = { headers: { "Content-Type": "application/json" } };
  http.post("http://localhost:8081/ticket", payload, params); // API'nin çalıştığı port
  sleep(0.1);
}
