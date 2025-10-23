<template>
  <div class="app-container">
    <el-tabs type="border-card" class="demo-tabs" >
      <!-- 网站信息 -->
      <el-tab-pane>
        <template #label>
                    <span class="custom-tabs-label">
                        <el-icon>
                            <Platform/>
                        </el-icon>
                        <span>网站信息</span>
                    </span>
        </template>
        <el-form label-width="80px" :model="siteConfig" label-position="left" class="config-form">
          <el-row gutter="20">
            <el-col :md="6">
              <el-form-item label="用户头像" class="upload-form-item">
                <el-upload class="avatar-uploader" :headers="authorization"
                           action="http://121.41.87.40:8080/admin/site/upload"
                           :show-file-list="false" accept="image/*" :before-upload="beforeUpload"
                           :on-success="handleUserAvatarSuccess">
                  <div class="avatar-container">
                    <img v-if="siteConfig.userAvatar" :src="siteConfig.userAvatar" class="avatar"/>
                    <el-icon v-else class="avatar-uploader-icon">
                      <Plus/>
                    </el-icon>
                    <div class="upload-mask">
                      <el-icon class="upload-icon"><Camera/></el-icon>
                    </div>
                  </div>
                </el-upload>
              </el-form-item>
            </el-col>
            <el-col :md="6">
              <el-form-item label="游客头像" class="upload-form-item">
                <el-upload class="avatar-uploader" :headers="authorization"
                           action="http://121.41.87.40:8080/admin/site/upload"
                           :show-file-list="false" accept="image/*" :before-upload="beforeUpload"
                           :on-success="handleTouristAvatarSuccess">
                  <div class="avatar-container">
                    <img v-if="siteConfig.touristAvatar" :src="siteConfig.touristAvatar" class="avatar"/>
                    <el-icon v-else class="avatar-uploader-icon">
                      <Plus/>
                    </el-icon>
                    <div class="upload-mask">
                      <el-icon class="upload-icon"><Camera/></el-icon>
                    </div>
                  </div>
                </el-upload>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="网站名称" class="input-form-item">
            <el-input v-model="siteConfig.siteName" style="width: 400px;" placeholder="请输入网站名称"></el-input>
          </el-form-item>
          <el-form-item label="网站地址" class="input-form-item">
            <el-input v-model="siteConfig.siteAddress" style="width: 400px;" placeholder="请输入网站地址"></el-input>
          </el-form-item>
          <el-form-item label="网站简介" class="input-form-item">
            <el-input v-model="siteConfig.siteIntro" style="width: 400px;" placeholder="请输入网站简介"></el-input>
          </el-form-item>
          <el-form-item label="网站公告" class="input-form-item">
            <el-input style="width: 400px;" v-model="siteConfig.siteNotice"
                      :autosize="{ minRows: 4, maxRows: 5 }" resize="none" type="textarea" placeholder="请输入网站公告"></el-input>
          </el-form-item>
          <el-form-item label="建站日期" class="input-form-item">
            <el-date-picker v-model="siteConfig.createSiteTime" value-format="YYYY-MM-DD" type="date"
                            placeholder="选择日期"></el-date-picker>
          </el-form-item>
          <el-form-item label="备案号" class="input-form-item">
            <el-input v-model="siteConfig.recordNumber" style="width: 400px;" placeholder="请输入备案号"></el-input>
          </el-form-item>
          <el-form-item class="form-actions">
            <el-button type="primary" @click="handleUpdate" class="submit-btn">保 存</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- 作者信息 -->
      <el-tab-pane label="author">
        <template #label>
                    <span class="custom-tabs-label">
                        <el-icon>
                            <Flag/>
                        </el-icon>
                        <span>作者信息</span>
                    </span>
        </template>
        <el-form label-width="80px" :model="siteConfig" label-position="left" class="config-form">
          <el-form-item label="作者头像" class="upload-form-item">
            <el-upload class="avatar-uploader" :headers="authorization"
                       action="http://121.41.87.40:8080/admin/site/upload"
                       :show-file-list="false" accept="image/*" :before-upload="beforeUpload"
                       :on-success="handleAuthorAvatarSuccess">
              <div class="avatar-container">
                <img v-if="siteConfig.authorAvatar" :src="siteConfig.authorAvatar" class="avatar"/>
                <el-icon v-else class="avatar-uploader-icon">
                  <Plus/>
                </el-icon>
                <div class="upload-mask">
                  <el-icon class="upload-icon"><Camera/></el-icon>
                </div>
              </div>
            </el-upload>
          </el-form-item>
          <el-form-item label="网站作者" class="input-form-item">
            <el-input v-model="siteConfig.siteAuthor" style="width: 400px;" placeholder="请输入网站作者"></el-input>
          </el-form-item>
          <el-form-item label="关于我" class="editor-form-item">
            <v-md-editor v-model="siteConfig.aboutMe" :disabled-menus="[]" :left-toolbar="toolList"
                         @upload-image="handleUploadImage" height="400px" class="md-editor"/>
          </el-form-item>
          <el-form-item class="form-actions">
            <el-button type="primary" @click="handleUpdate" class="submit-btn">保 存</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- 社交信息 -->
      <el-tab-pane label="social">
        <template #label>
                    <span class="custom-tabs-label">
                        <el-icon>
                            <Opportunity/>
                        </el-icon>
                        <span>社交信息</span>
                    </span>
        </template>
        <el-form label-width="70px" :model="siteConfig" label-position="left" class="config-form">
          <el-checkbox-group v-model="socialList" class="checkbox-group">
            <el-form-item label="Github" class="input-form-item">
              <el-input v-model="siteConfig.github" style="width: 400px; margin-right: 1rem" placeholder="请输入Github地址"></el-input>
              <el-checkbox label="github" class="display-checkbox">是否展示</el-checkbox>
            </el-form-item>
            <el-form-item label="Gitee" class="input-form-item">
              <el-input v-model="siteConfig.gitee" style="width: 400px; margin-right: 1rem" placeholder="请输入Gitee地址"></el-input>
              <el-checkbox label="gitee" class="display-checkbox">是否展示</el-checkbox>
            </el-form-item>
            <el-form-item label="BiliBili" class="input-form-item">
              <el-input v-model="siteConfig.bilibili" style="width: 400px; margin-right: 1rem" placeholder="请输入BiliBili地址"></el-input>
              <el-checkbox label="bilibili" class="display-checkbox">是否展示</el-checkbox>
            </el-form-item>
            <el-form-item label="QQ" class="input-form-item">
              <el-input v-model="siteConfig.qq" style="width: 400px; margin-right: 1rem" placeholder="请输入QQ号码"></el-input>
              <el-checkbox label="qq" class="display-checkbox">是否展示</el-checkbox>
            </el-form-item>
            <el-form-item class="form-actions">
              <el-button type="primary" @click="handleUpdate" class="submit-btn">保 存</el-button>
            </el-form-item>
          </el-checkbox-group>
        </el-form>
      </el-tab-pane>

      <!-- 审核&打赏 -->
      <el-tab-pane label="check">
        <template #label>
                    <span class="custom-tabs-label">
                        <el-icon>
                            <Stamp/>
                        </el-icon>
                        <span>审核&打赏</span>
                    </span>
        </template>
        <el-form label-width="100px" :model="siteConfig" label-position="left" class="config-form">
          <el-form-item label="评论审核" class="radio-form-item">
            <el-radio-group v-model="siteConfig.commentCheck" class="radio-group">
              <el-radio :label="0">关闭</el-radio>
              <el-radio :label="1">开启</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="留言审核" class="radio-form-item">
            <el-radio-group v-model="siteConfig.messageCheck" class="radio-group">
              <el-radio :label="0">关闭</el-radio>
              <el-radio :label="1">开启</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="打赏状态" class="radio-form-item">
            <el-radio-group v-model="siteConfig.isReward" class="radio-group">
              <el-radio :label="0">关闭</el-radio>
              <el-radio :label="1">开启</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-row style="width: 600px" v-if="siteConfig.isReward == 1" class="reward-row">
            <el-col :md="12">
              <el-form-item label="微信收款码" class="upload-form-item">
                <el-upload class="avatar-uploader" :headers="authorization"
                           action="http://121.41.87.40:8080/admin/site/upload"
                           :show-file-list="false" accept="image/*" :before-upload="beforeUpload"
                           :on-success="handleWeiXinSuccess">
                  <div class="avatar-container">
                    <img v-if="siteConfig.weiXinCode" :src="siteConfig.weiXinCode" class="avatar"/>
                    <el-icon v-else class="avatar-uploader-icon">
                      <Plus/>
                    </el-icon>
                    <div class="upload-mask">
                      <el-icon class="upload-icon"><Camera/></el-icon>
                    </div>
                  </div>
                </el-upload>
              </el-form-item>
            </el-col>
            <el-col :md="12">
              <el-form-item label="支付宝收款码" class="upload-form-item">
                <el-upload class="avatar-uploader" :headers="authorization"
                           action="http://121.41.87.40:8080/admin/site/upload"
                           :show-file-list="false" accept="image/*" :before-upload="beforeUpload"
                           :on-success="handleAliSuccess">
                  <div class="avatar-container">
                    <img v-if="siteConfig.aliCode" :src="siteConfig.aliCode" class="avatar"/>
                    <el-icon v-else class="avatar-uploader-icon">
                      <Plus/>
                    </el-icon>
                    <div class="upload-mask">
                      <el-icon class="upload-icon"><Camera/></el-icon>
                    </div>
                  </div>
                </el-upload>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item class="form-actions">
            <el-button type="primary" @click="handleUpdate" class="submit-btn">保 存</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- 其他设置 -->
      <el-tab-pane label="other">
        <template #label>
                    <span class="custom-tabs-label">
                        <el-icon>
                            <Briefcase/>
                        </el-icon>
                        <span>其他设置</span>
                    </span>
        </template>
        <el-form label-width="100px" :model="siteConfig" label-position="left" class="config-form">
          <el-form-item label="文章默认封面" class="upload-form-item article-cover-item">
            <el-upload class="avatar-uploader" :headers="authorization"
                       action="http://121.41.87.40:8080/admin/site/upload"
                       :show-file-list="false" accept="image/*" :before-upload="beforeUpload"
                       :on-success="handleArticleSuccess">
              <div class="article-cover-container">
                <img v-if="siteConfig.articleCover" :src="siteConfig.articleCover" class="article-cover"/>
                <el-icon v-else class="avatar-uploader-icon article-cover-icon">
                  <Plus/>
                </el-icon>
                <div class="upload-mask article-cover-mask">
                  <el-icon class="upload-icon"><Camera/></el-icon>
                </div>
              </div>
            </el-upload>
          </el-form-item>
          <el-form-item label="邮箱通知" class="radio-form-item">
            <el-radio-group v-model="siteConfig.emailNotice" class="radio-group">
              <el-radio :label="0">关闭</el-radio>
              <el-radio :label="1">开启</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="第三方登录" class="checkbox-form-item">
            <el-checkbox-group v-model="loginList" class="login-checkbox-group">
              <el-checkbox label="qq" class="login-checkbox">QQ</el-checkbox>
              <el-checkbox label="gitee" class="login-checkbox">Gitee</el-checkbox>
              <el-checkbox label="github" class="login-checkbox">Github</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="音乐播放器" class="radio-form-item">
            <el-radio-group v-model="siteConfig.isMusic" class="radio-group">
              <el-radio :label="0">关闭</el-radio>
              <el-radio :label="1">开启</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="网易云歌单Id" v-if="siteConfig.isMusic == 1" class="input-form-item">
            <el-input v-model="siteConfig.musicId" style="width: 400px;" placeholder="请输入网易云歌单ID"></el-input>
          </el-form-item>
          <el-form-item class="form-actions">
            <el-button type="primary" @click="handleUpdate" class="submit-btn">保 存</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import {getSiteConfig, updateSiteConfig, uploadSiteImg} from '@/api/site';
import {SiteConfig} from '@/api/site/types';
import {notifySuccess} from '@/utils/modal';
import {getToken, token_prefix} from '@/utils/token';
import {AxiosResponse} from 'axios';
import {UploadRawFile} from 'element-plus';
import * as imageConversion from 'image-conversion';
import {computed, onMounted, reactive, toRefs} from 'vue';
// 引入需要的图标
import { Platform, Flag, Opportunity, Stamp, Briefcase, Plus, Camera } from '@element-plus/icons-vue';

const toolList = "undo redo clear | h bold italic strikethrough quote | ul ol table hr | link image code | emoji tip todo-list";
const authorization = computed(() => {
  return {
    Authorization: token_prefix + getToken(),
  }
});
const data = reactive({
  siteConfig: {} as SiteConfig,
  socialList: [] as string[],
  loginList: [] as string[],
});
const {
  siteConfig,
  socialList,
  loginList,
} = toRefs(data);
const handleUploadImage = (event: any, insertImage: any, files: File[]) => {
  files.forEach(file => {
    let formData = new FormData();
    formData.append("file", file);
    uploadSiteImg(formData).then(({data}) => {
      if (data.flag) {
        insertImage({
          url: data.data,
        });
      }
    })
  });
};
const handleUserAvatarSuccess = (response: AxiosResponse) => {
  siteConfig.value.userAvatar = response.data;
};
const handleTouristAvatarSuccess = (response: AxiosResponse) => {
  siteConfig.value.touristAvatar = response.data;
};
const handleAuthorAvatarSuccess = (response: AxiosResponse) => {
  siteConfig.value.authorAvatar = response.data;
};
const handleWeiXinSuccess = (response: AxiosResponse) => {
  siteConfig.value.weiXinCode = response.data;
};
const handleAliSuccess = (response: AxiosResponse) => {
  siteConfig.value.aliCode = response.data;
};
const handleArticleSuccess = (response: AxiosResponse) => {
  siteConfig.value.articleCover = response.data;
};
const beforeUpload = (rawFile: UploadRawFile) => {
  return new Promise(resolve => {
    if (rawFile.size / 1024 < 200) {
      resolve(rawFile);
    }
    // 压缩到200KB,这里的200就是要压缩的大小,可自定义
    imageConversion
        .compressAccurately(rawFile, 200)
        .then(res => {
          resolve(res);
        });
  });
};
const handleUpdate = () => {
  if (loginList.value.length > 0) {
    siteConfig.value.loginList = loginList.value.toString();
  } else {
    siteConfig.value.loginList = "";
  }
  if (socialList.value.length > 0) {
    siteConfig.value.socialList = socialList.value.toString();
  } else {
    siteConfig.value.socialList = "";
  }
  updateSiteConfig(siteConfig.value).then(({data}) => {
    if (data.flag) {
      notifySuccess(data.msg);
      getList();
    }
  })
};
const getList = () => {
  getSiteConfig().then(({data}) => {
    siteConfig.value = data.data;
    socialList.value = data.data.socialList.split(",").filter(Boolean);
    loginList.value = data.data.loginList.split(",").filter(Boolean);
  })
};
onMounted(() => {
  getList();
});
</script>

<style scoped>
.app-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.demo-tabs {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.demo-tabs > .el-tabs__content {
  padding: 24px 32px;
  color: #6b778c;
}

/* 标签样式优化 */
.demo-tabs .el-tabs__nav {
  padding-left: 16px;
}

.demo-tabs .custom-tabs-label {
  display: inline-flex;
  align-items: center;
  padding: 0 16px;
  border-radius: 4px 4px 0 0;
  transition: all 0.2s ease;
}

.demo-tabs .custom-tabs-label:hover {
  background-color: #f5f7fa;
}

.demo-tabs .custom-tabs-label .el-icon {
  font-size: 16px;
  color: #409eff;
}

.demo-tabs .custom-tabs-label span {
  margin-left: 6px;
  font-size: 14px;
  font-weight: 500;
}

/* 表单样式 */
.config-form {
  margin-top: 10px;
}

.input-form-item,
.radio-form-item,
.checkbox-form-item,
.upload-form-item,
.editor-form-item {
  margin-bottom: 20px;
  transition: all 0.2s ease;
}

.input-form-item:hover,
.radio-form-item:hover,
.checkbox-form-item:hover,
.upload-form-item:hover,
.editor-form-item:hover {
  background-color: rgba(64, 158, 255, 0.03);
}

/* 输入框样式 */
.el-input,
.el-date-picker {
  border-radius: 4px;
  transition: all 0.3s ease;
}

.el-input:focus-within,
.el-date-picker:focus-within {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

/* 单选框样式 */
.radio-group {
  display: flex;
  gap: 20px;
}

.el-radio {
  padding: 6px 12px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.el-radio:hover {
  background-color: #f5f7fa;
}

.el-radio__input.is-checked + .el-radio__label {
  color: #409eff;
  font-weight: 500;
}

/* 复选框样式 */
.login-checkbox-group {
  display: flex;
  gap: 20px;
}

.login-checkbox {
  padding: 6px 12px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.login-checkbox:hover {
  background-color: #f5f7fa;
}

.display-checkbox {
  padding: 6px 12px;
  border-radius: 4px;
}

/* 上传组件样式 */
.avatar-container {
  position: relative;
  width: 120px;
  height: 120px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s ease;
  cursor: pointer;
}

.avatar-container:hover {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.avatar {
  width: 120px;
  height: 120px;
  object-fit: contain;
}

.avatar-uploader-icon {
  width: 120px;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #8c8c8c;
}

.upload-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.avatar-container:hover .upload-mask {
  opacity: 1;
}

.upload-icon {
  color: #fff;
  font-size: 20px;
}

/* 文章封面样式 */
.article-cover-item {
  margin-bottom: 20px;
}

.article-cover-container {
  position: relative;
  width: 300px;
  height: 180px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s ease;
  cursor: pointer;
}

.article-cover-container:hover {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.article-cover {
  width: 300px;
  height: 180px;
  object-fit: contain;
}

.article-cover-icon {
  width: 300px;
  height: 180px;
  font-size: 32px;
}

.article-cover-mask {
  height: 180px;
}

/* 按钮样式 */
.form-actions {
  margin-top: 30px;
  padding-top: 15px;
  border-top: 1px solid #f0f0f0;
}

.submit-btn {
  padding: 10px 24px;
  font-size: 14px;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.submit-btn:active {
  transform: translateY(0);
}

/* 富文本编辑器样式 */
.md-editor {
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  transition: all 0.3s ease;
}

.md-editor:hover {
  border-color: #c0c4cc;
}

/* 打赏区域样式 */
.reward-row {
  margin-top: 10px;
  padding: 15px;
  background-color: #f9f9f9;
  border-radius: 8px;
}
</style>