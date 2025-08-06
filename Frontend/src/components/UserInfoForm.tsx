import React, { useState } from 'react';

interface UserInfo {
  name: string;
  email: string;
  phone: string;
  address: string;
  city: string;
  country: string;
}

interface UserInfoResponse {
  success: boolean;
  message: string;
  userId?: string;
  userInfo?: UserInfo;
}

const UserInfoForm: React.FC = () => {
  const [formData, setFormData] = useState<UserInfo>({
    name: '',
    email: '',
    phone: '',
    address: '',
    city: '',
    country: ''
  });
  const [submitting, setSubmitting] = useState(false);
  const [submitResult, setSubmitResult] = useState<UserInfoResponse | null>(null);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setSubmitResult(null);

    try {
      const response = await fetch('http://localhost:8080/api/user-info', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result: UserInfoResponse = await response.json();
      setSubmitResult(result);

      if (result.success) {
        // Clear form on success
        setFormData({
          name: '',
          email: '',
          phone: '',
          address: '',
          city: '',
          country: ''
        });
      }

    } catch (error) {
      console.error('Submit failed:', error);
      setSubmitResult({
        success: false,
        message: 'Failed to submit information: ' + (error as Error).message
      });
    } finally {
      setSubmitting(false);
    }
  };

  const isFormValid = formData.name && formData.email;

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div className="bg-white p-6 rounded-lg shadow-sm border">
        <h2 className="text-xl font-semibold mb-4">👤 User Information Form</h2>
        <p className="text-gray-600 mb-6">
          Fill out the form below to store your information in MongoDB Atlas.
        </p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Name */}
            <div>
              <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
                Full Name *
              </label>
              <input
                type="text"
                id="name"
                name="name"
                value={formData.name}
                onChange={handleInputChange}
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="Enter your full name"
              />
            </div>

            {/* Email */}
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
                Email Address *
              </label>
              <input
                type="email"
                id="email"
                name="email"
                value={formData.email}
                onChange={handleInputChange}
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="Enter your email address"
              />
            </div>

            {/* Phone */}
            <div>
              <label htmlFor="phone" className="block text-sm font-medium text-gray-700 mb-1">
                Phone Number
              </label>
              <input
                type="tel"
                id="phone"
                name="phone"
                value={formData.phone}
                onChange={handleInputChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="Enter your phone number"
              />
            </div>

            {/* City */}
            <div>
              <label htmlFor="city" className="block text-sm font-medium text-gray-700 mb-1">
                City
              </label>
              <input
                type="text"
                id="city"
                name="city"
                value={formData.city}
                onChange={handleInputChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="Enter your city"
              />
            </div>
          </div>

          {/* Address */}
          <div>
            <label htmlFor="address" className="block text-sm font-medium text-gray-700 mb-1">
              Address
            </label>
            <textarea
              id="address"
              name="address"
              value={formData.address}
              onChange={handleInputChange}
              rows={3}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="Enter your address"
            />
          </div>

          {/* Country */}
          <div>
            <label htmlFor="country" className="block text-sm font-medium text-gray-700 mb-1">
              Country
            </label>
            <input
              type="text"
              id="country"
              name="country"
              value={formData.country}
              onChange={handleInputChange}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="Enter your country"
            />
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            disabled={!isFormValid || submitting}
            className="w-full bg-blue-600 text-white py-3 px-4 rounded-lg font-medium hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors"
          >
            {submitting ? (
              <span className="flex items-center justify-center space-x-2">
                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                <span>Saving to MongoDB...</span>
              </span>
            ) : (
              'Save Information'
            )}
          </button>
        </form>
      </div>

      {/* Submit Result */}
      {submitResult && (
        <div className="bg-white p-6 rounded-lg shadow-sm border">
          <h3 className="text-lg font-semibold mb-4">
            {submitResult.success ? '✅ Success!' : '❌ Error'}
          </h3>
          
          <div className={`p-4 rounded-lg ${
            submitResult.success 
              ? 'bg-green-50 border border-green-200 text-green-800' 
              : 'bg-red-50 border border-red-200 text-red-800'
          }`}>
            <p className="font-medium">{submitResult.message}</p>
            
            {submitResult.success && submitResult.userId && (
              <div className="mt-3 text-sm">
                <p><strong>User ID:</strong> {submitResult.userId}</p>
                <p><strong>Stored in:</strong> MongoDB Atlas</p>
                <p><strong>Collection:</strong> users</p>
              </div>
            )}
          </div>

          {submitResult.success && submitResult.userInfo && (
            <div className="mt-4 p-4 bg-blue-50 border border-blue-200 rounded-lg">
              <h4 className="font-medium text-blue-900 mb-2">Saved Information:</h4>
              <div className="text-sm text-blue-800 space-y-1">
                <p><strong>Name:</strong> {submitResult.userInfo.name}</p>
                <p><strong>Email:</strong> {submitResult.userInfo.email}</p>
                {submitResult.userInfo.phone && <p><strong>Phone:</strong> {submitResult.userInfo.phone}</p>}
                {submitResult.userInfo.city && <p><strong>City:</strong> {submitResult.userInfo.city}</p>}
                {submitResult.userInfo.country && <p><strong>Country:</strong> {submitResult.userInfo.country}</p>}
                {submitResult.userInfo.address && <p><strong>Address:</strong> {submitResult.userInfo.address}</p>}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default UserInfoForm;
